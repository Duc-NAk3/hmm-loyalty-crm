package vn.sofitech.hmm.pos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.AdminFacade;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.catalog.CatalogFacade;
import vn.sofitech.hmm.catalog.CatalogFacade.ProductLabel;
import vn.sofitech.hmm.catalog.CatalogFacade.SellableProduct;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.customer.CustomerFacade.CustomerInfo;
import vn.sofitech.hmm.loyalty.LoyaltyFacade;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.EarnResult;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionInfo;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.pos.domain.*;
import vn.sofitech.hmm.pos.web.PosDtos.*;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hoàn tất hóa đơn bán hàng (FR-02, FR-04, FR-28): định giá lại từ danh mục trung tâm, thanh toán tiền mặt + điểm,
 * xác nhận đổi điểm, tích điểm, cập nhật hạng – tất cả trong một giao dịch CSDL.
 * Gọi module khác chỉ qua Facade: CatalogFacade, CustomerFacade, LoyaltyFacade, AdminFacade.
 */
@Service
public class SaleService {

    /** Dòng hàng đã định giá theo giá đang hiệu lực của cửa hàng. */
    public record PricedLine(SellableProduct product, BigDecimal quantity, long amount) {
    }

    private final SalesTransactionRepository transactions;
    private final ShiftService shifts;
    private final CatalogFacade catalog;
    private final CustomerFacade customers;
    private final LoyaltyFacade loyalty;
    private final AdminFacade admin;

    public SaleService(SalesTransactionRepository transactions, ShiftService shifts, CatalogFacade catalog, CustomerFacade customers,
                       LoyaltyFacade loyalty, AdminFacade admin) {
        this.transactions = transactions;
        this.shifts = shifts;
        this.catalog = catalog;
        this.customers = customers;
        this.loyalty = loyalty;
        this.admin = admin;
    }

    @Transactional(readOnly = true)
    public List<PricedLine> price(List<LineInput> lines, long storeId) {
        if (lines == null || lines.isEmpty()) throw ApiException.badRequest("EMPTY", "Hóa đơn chưa có mặt hàng");
        Map<Long, SellableProduct> products = catalog.sellableByIds(lines.stream().map(LineInput::productId).toList(), storeId);
        List<PricedLine> out = new ArrayList<>();
        for (LineInput l : lines) {
            SellableProduct p = products.get(l.productId());
            if (p == null) throw ApiException.badRequest("PRODUCT", "Mặt hàng " + l.productId() + " không tồn tại hoặc ngừng bán");
            if (p.price() < 0) throw ApiException.badRequest("NO_PRICE", "Mặt hàng " + p.name() + " chưa có giá bán");
            if (l.quantity() == null || l.quantity().signum() <= 0) throw ApiException.badRequest("QTY", "Số lượng phải lớn hơn 0");
            long amount = l.quantity().multiply(BigDecimal.valueOf(p.price())).setScale(0, RoundingMode.HALF_UP).longValueExact();
            out.add(new PricedLine(p, l.quantity(), amount));
        }
        return out;
    }

    /** Giá trị các dòng được tích điểm / được trừ bằng điểm (loại hàng hạn chế khuyến mại – BRL-05). */
    public static long redeemableAmount(List<PricedLine> lines) {
        return lines.stream().filter(l -> l.product().pointEligible()).mapToLong(PricedLine::amount).sum();
    }

    @Transactional
    public ReceiptView checkout(CurrentUser cashier, SaleRequest req) {
        if (Texts.isBlank(req.receiptNo())) throw ApiException.badRequest("RECEIPT_NO", "Thiếu mã hóa đơn");
        // Chống trùng: gửi lại cùng mã hóa đơn thì trả kết quả cũ, không ghi lần hai
        Optional<SalesTransaction> existing = transactions.findByReceiptNo(req.receiptNo());
        if (existing.isPresent()) return receipt(existing.get(), null, null);

        shifts.checkTerminal(cashier, req.terminalId());
        Shift shift = shifts.openShift(req.terminalId()).filter(s -> s.getId() == req.shiftId())
                .orElseThrow(() -> ApiException.conflict("NO_SHIFT", "Ca bán hàng chưa mở hoặc đã đóng"));
        List<PricedLine> lines = price(req.lines(), cashier.storeId());

        CustomerInfo member = null;
        if (req.customerId() != null)
            member = customers.findById(req.customerId()).filter(c -> !"MERGED".equals(c.status()))
                    .orElseThrow(() -> ApiException.badRequest("MEMBER", "Thành viên không hợp lệ"));
        if (req.redemptionId() != null && member == null) throw ApiException.badRequest("MEMBER", "Đổi điểm cần gắn thành viên vào hóa đơn");

        OffsetDateTime now = OffsetDateTime.now(Texts.VN);
        SalesTransaction txn = SalesTransaction.sale(req.receiptNo(), cashier.storeId(), req.terminalId(), shift.getId(), cashier.id(),
                member == null ? null : member.customerId(), now);
        lines.forEach(l -> txn.addLine(l.product().productId(), l.quantity(), l.product().price(), l.amount(), l.product().pointEligible()));
        // Ghi hóa đơn trước: sổ cái điểm và lô điểm tham chiếu transaction_id
        transactions.saveAndFlush(txn);

        long redeemable = txn.redeemableAmount();
        int pointsRedeemed = 0;
        long pointsAmount = 0;
        if (req.redemptionId() != null) {
            PointCalculator.Policy policy = loyalty.currentPolicy();
            RedemptionInfo r = loyalty.confirmRedemption(req.redemptionId(), member.customerId(), req.terminalId(), txn.getId(), cashier.id());
            if (r.amountVnd() > redeemable * policy.maxRedeemPct() / 100)
                throw ApiException.badRequest("REDEEM_LIMIT", "Số tiền trừ bằng điểm vượt " + policy.maxRedeemPct()
                        + "% giá trị hàng được đổi điểm, vui lòng đổi lại");
            pointsRedeemed = r.points();
            pointsAmount = r.amountVnd();
            txn.addPayment(Payment.Method.POINTS, pointsAmount, r.redemptionId(), null);
        }
        long cashDue = txn.getNetAmount() - pointsAmount;
        if (req.cashTendered() < cashDue) throw ApiException.badRequest("CASH", "Tiền khách đưa chưa đủ");
        if (cashDue > 0) txn.addPayment(Payment.Method.CASH, cashDue, null, "tendered=" + req.cashTendered());

        String upgradedTo = null;
        if (member != null) {
            long eligible = Math.max(0, redeemable - pointsAmount); // BRL-03: không tính phần trả bằng điểm
            EarnResult earn = loyalty.earnForSale(member.customerId(), txn.getId(), eligible, txn.getNetAmount(), now, cashier.id());
            txn.applyLoyalty(eligible, earn.pointsEarned(), pointsRedeemed);
            upgradedTo = earn.upgradedToName();
        }
        return receipt(txn, req.cashTendered(), upgradedTo);
    }

    private ReceiptView receipt(SalesTransaction t, Long cashTendered, String upgradedTo) {
        AdminFacade.StoreInfo store = admin.findStore(t.getStoreId()).orElseThrow();
        String terminalCode = t.getTerminalId() == null ? null
                : admin.findTerminal(t.getTerminalId()).map(AdminFacade.TerminalInfo::terminalCode).orElse(null);
        String cashierName = t.getCashierId() == null ? null
                : admin.findUser(t.getCashierId()).map(AdminFacade.UserInfo::fullName).orElse(null);
        CustomerInfo member = t.getCustomerId() == null ? null : customers.findById(t.getCustomerId()).orElse(null);
        Map<Long, ProductLabel> labels = catalog.labels(t.getLines().stream().map(TransactionLine::getProductId).toList());
        List<ReceiptLineView> lines = t.getLines().stream().map(l -> {
            ProductLabel p = labels.get(l.getProductId());
            return new ReceiptLineView(p.sku(), p.name(), p.unit(), l.getQuantity(), l.getUnitPrice(), l.getLineAmount(), l.isPointEligible());
        }).toList();
        List<PaymentView> payments = t.getPayments().stream().map(p -> new PaymentView(p.getMethod().name(), p.getAmount())).toList();
        Long change = cashTendered == null ? null : cashTendered - t.cashPaid();
        return new ReceiptView(t.getId(), t.getReceiptNo(), t.getTxnTime(), t.getNetAmount(), t.getEligibleAmount(), t.getPointsEarned(),
                t.getPointsRedeemed(), store.storeCode(), store.name(), store.address(), terminalCode, cashierName,
                member == null ? null : member.memberCode(), member == null ? null : member.fullName(),
                member == null ? null : Texts.maskPhone(member.phone()), member == null ? null : member.pointBalance(),
                member == null ? null : loyalty.tierName(member.tierCode()), lines, payments, cashTendered, change, upgradedTo);
    }
}
