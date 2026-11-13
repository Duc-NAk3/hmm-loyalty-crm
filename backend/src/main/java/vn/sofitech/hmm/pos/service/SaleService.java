package vn.sofitech.hmm.pos.service;

import vn.sofitech.hmm.admin.api.TerminalInfo;
import vn.sofitech.hmm.admin.api.StoreInfo;
import vn.sofitech.hmm.admin.api.UserInfo;
import vn.sofitech.hmm.pos.dto.sale.LineInput;
import vn.sofitech.hmm.pos.dto.sale.SaleRequest;
import vn.sofitech.hmm.pos.dto.sale.ReceiptLineView;
import vn.sofitech.hmm.pos.dto.sale.PaymentView;
import vn.sofitech.hmm.pos.dto.sale.ReceiptView;
import vn.sofitech.hmm.pos.repository.SalesTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.AdminFacade;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.catalog.CatalogFacade;
import vn.sofitech.hmm.catalog.api.ProductLabel;
import vn.sofitech.hmm.catalog.api.SellableProduct;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.customer.api.CustomerInfo;
import vn.sofitech.hmm.loyalty.LoyaltyFacade;
import vn.sofitech.hmm.loyalty.api.EarnResult;
import vn.sofitech.hmm.loyalty.api.RedemptionInfo;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.pos.domain.*;
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
        Map<Long, SellableProduct> products = catalog.sellableByIds(lines.stream().map(LineInput::getProductId).toList(), storeId);
        List<PricedLine> out = new ArrayList<>();
        for (LineInput l : lines) {
            SellableProduct p = products.get(l.getProductId());
            if (p == null) throw ApiException.badRequest("PRODUCT", "Mặt hàng " + l.getProductId() + " không tồn tại hoặc ngừng bán");
            if (p.getPrice() < 0) throw ApiException.badRequest("NO_PRICE", "Mặt hàng " + p.getName() + " chưa có giá bán");
            if (l.getQuantity() == null || l.getQuantity().signum() <= 0) throw ApiException.badRequest("QTY", "Số lượng phải lớn hơn 0");
            long amount = l.getQuantity().multiply(BigDecimal.valueOf(p.getPrice())).setScale(0, RoundingMode.HALF_UP).longValueExact();
            out.add(new PricedLine(p, l.getQuantity(), amount));
        }
        return out;
    }

    /** Giá trị các dòng được tích điểm / được trừ bằng điểm (loại hàng hạn chế khuyến mại – BRL-05). */
    public static long redeemableAmount(List<PricedLine> lines) {
        return lines.stream().filter(l -> l.product().pointEligible()).mapToLong(PricedLine::amount).sum();
    }

    @Transactional
    public ReceiptView checkout(CurrentUser cashier, SaleRequest req) {
        if (Texts.isBlank(req.getReceiptNo())) throw ApiException.badRequest("RECEIPT_NO", "Thiếu mã hóa đơn");
        // Chống trùng: gửi lại cùng mã hóa đơn thì trả kết quả cũ, không ghi lần hai
        Optional<SalesTransaction> existing = transactions.findByReceiptNo(req.getReceiptNo());
        if (existing.isPresent()) return receipt(existing.get(), null, null);

        shifts.checkTerminal(cashier, req.getTerminalId());
        Shift shift = shifts.openShift(req.getTerminalId()).filter(s -> s.getId() == req.getShiftId())
                .orElseThrow(() -> ApiException.conflict("NO_SHIFT", "Ca bán hàng chưa mở hoặc đã đóng"));
        List<PricedLine> lines = price(req.getLines(), cashier.storeId());

        CustomerInfo member = null;
        if (req.getCustomerId() != null)
            member = customers.findById(req.getCustomerId()).filter(c -> !"MERGED".equals(c.getStatus()))
                    .orElseThrow(() -> ApiException.badRequest("MEMBER", "Thành viên không hợp lệ"));
        if (req.getRedemptionId() != null && member == null) throw ApiException.badRequest("MEMBER", "Đổi điểm cần gắn thành viên vào hóa đơn");

        OffsetDateTime now = OffsetDateTime.now(Texts.VN);
        SalesTransaction txn = SalesTransaction.sale(req.getReceiptNo(), cashier.storeId(), req.getTerminalId(), shift.getId(), cashier.id(),
                member == null ? null : member.getCustomerId(), now);
        lines.forEach(l -> txn.addLine(l.product().getProductId(), l.quantity(), l.product().getPrice(), l.amount(), l.product().pointEligible()));
        // Ghi hóa đơn trước: sổ cái điểm và lô điểm tham chiếu transaction_id
        transactions.saveAndFlush(txn);

        long redeemable = txn.redeemableAmount();
        int pointsRedeemed = 0;
        long pointsAmount = 0;
        if (req.getRedemptionId() != null) {
            PointCalculator.Policy policy = loyalty.currentPolicy();
            RedemptionInfo r = loyalty.confirmRedemption(req.getRedemptionId(), member.getCustomerId(), req.getTerminalId(), txn.getId(), cashier.id());
            if (r.getAmountVnd() > redeemable * policy.maxRedeemPct() / 100)
                throw ApiException.badRequest("REDEEM_LIMIT", "Số tiền trừ bằng điểm vượt " + policy.maxRedeemPct()
                        + "% giá trị hàng được đổi điểm, vui lòng đổi lại");
            pointsRedeemed = r.getPoints();
            pointsAmount = r.getAmountVnd();
            txn.addPayment(Payment.Method.POINTS, pointsAmount, r.getRedemptionId(), null);
        }
        long cashDue = txn.getNetAmount() - pointsAmount;
        if (req.getCashTendered() < cashDue) throw ApiException.badRequest("CASH", "Tiền khách đưa chưa đủ");
        if (cashDue > 0) txn.addPayment(Payment.Method.CASH, cashDue, null, "tendered=" + req.getCashTendered());

        String upgradedTo = null;
        if (member != null) {
            long eligible = Math.max(0, redeemable - pointsAmount); // BRL-03: không tính phần trả bằng điểm
            EarnResult earn = loyalty.earnForSale(member.getCustomerId(), txn.getId(), eligible, txn.getNetAmount(), now, cashier.id());
            txn.applyLoyalty(eligible, earn.getPointsEarned(), pointsRedeemed);
            upgradedTo = earn.getUpgradedToName();
        }
        return receipt(txn, req.getCashTendered(), upgradedTo);
    }

    private ReceiptView receipt(SalesTransaction t, Long cashTendered, String upgradedTo) {
        StoreInfo store = admin.findStore(t.getStoreId()).orElseThrow();
        String terminalCode = t.getTerminalId() == null ? null
                : admin.findTerminal(t.getTerminalId()).map(TerminalInfo::getTerminalCode).orElse(null);
        String cashierName = t.getCashierId() == null ? null
                : admin.findUser(t.getCashierId()).map(UserInfo::getFullName).orElse(null);
        CustomerInfo member = t.getCustomerId() == null ? null : customers.findById(t.getCustomerId()).orElse(null);
        Map<Long, ProductLabel> labels = catalog.labels(t.getLines().stream().map(TransactionLine::getProductId).toList());
        List<ReceiptLineView> lines = t.getLines().stream().map(l -> {
            ProductLabel p = labels.get(l.getProductId());
            return new ReceiptLineView(p.getSku(), p.getName(), p.getUnit(), l.getQuantity(), l.getUnitPrice(), l.getLineAmount(), l.isPointEligible());
        }).toList();
        List<PaymentView> payments = t.getPayments().stream().map(p -> new PaymentView(p.getMethod().name(), p.getAmount())).toList();
        Long change = cashTendered == null ? null : cashTendered - t.cashPaid();
        return new ReceiptView(t.getId(), t.getReceiptNo(), t.getTxnTime(), t.getNetAmount(), t.getEligibleAmount(), t.getPointsEarned(),
                t.getPointsRedeemed(), store.getStoreCode(), store.getName(), store.getAddress(), terminalCode, cashierName,
                member == null ? null : member.getMemberCode(), member == null ? null : member.getFullName(),
                member == null ? null : Texts.maskPhone(member.getPhone()), member == null ? null : member.getPointBalance(),
                member == null ? null : loyalty.tierName(member.getTierCode()), lines, payments, cashTendered, change, upgradedTo);
    }
}
