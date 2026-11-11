package vn.sofitech.hmm.pos;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.catalog.CatalogService;
import vn.sofitech.hmm.customer.CustomerService;
import vn.sofitech.hmm.loyalty.*;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Hoàn tất hóa đơn bán hàng (FR-02, FR-04, FR-28): định giá lại từ danh mục trung tâm, thanh toán tiền mặt + điểm,
 * xác nhận đổi điểm, tích điểm, cập nhật hạng – tất cả trong một giao dịch CSDL. receipt_no do quầy sinh là khóa chống trùng.
 */
@Service
public class SaleService {

    public record LineInput(long productId, BigDecimal quantity) {
    }

    public record SaleRequest(long terminalId, long shiftId, String receiptNo, Long customerId, List<LineInput> lines,
                              Long redemptionId, long cashTendered) {
    }

    /** Dòng hàng đã định giá. */
    public record PricedLine(CatalogService.SellableProduct product, BigDecimal quantity, long amount) {
    }

    private final JdbcClient jdbc;
    private final CatalogService catalog;
    private final CustomerService customers;
    private final LoyaltyConfigService config;
    private final LedgerService ledger;
    private final RedemptionService redemptions;
    private final TierService tiers;
    private final ShiftService shifts;

    public SaleService(JdbcClient jdbc, CatalogService catalog, CustomerService customers, LoyaltyConfigService config,
                       LedgerService ledger, RedemptionService redemptions, TierService tiers, ShiftService shifts) {
        this.jdbc = jdbc;
        this.catalog = catalog;
        this.customers = customers;
        this.config = config;
        this.ledger = ledger;
        this.redemptions = redemptions;
        this.tiers = tiers;
        this.shifts = shifts;
    }

    /** Định giá danh sách dòng hàng theo giá đang hiệu lực của cửa hàng. */
    public List<PricedLine> price(List<LineInput> lines, long storeId) {
        if (lines == null || lines.isEmpty()) throw ApiException.badRequest("EMPTY", "Hóa đơn chưa có mặt hàng");
        Map<Long, CatalogService.SellableProduct> products = catalog.sellableByIds(lines.stream().map(LineInput::productId).toList(), storeId);
        List<PricedLine> out = new ArrayList<>();
        for (LineInput l : lines) {
            CatalogService.SellableProduct p = products.get(l.productId());
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
    public Map<String, Object> checkout(CurrentUser cashier, SaleRequest req) {
        if (Texts.isBlank(req.receiptNo())) throw ApiException.badRequest("RECEIPT_NO", "Thiếu mã hóa đơn");
        Optional<Long> existing = jdbc.sql("SELECT transaction_id FROM pos.sales_transactions WHERE receipt_no = :r")
                .param("r", req.receiptNo()).query(Long.class).optional();
        if (existing.isPresent()) return receipt(existing.get(), null); // gửi lại cùng mã: trả kết quả cũ, không ghi trùng

        shifts.checkTerminal(cashier, req.terminalId());
        long storeId = cashier.storeId();
        Map<String, Object> shift = shifts.current(req.terminalId())
                .filter(s -> ((Number) s.get("shift_id")).longValue() == req.shiftId())
                .orElseThrow(() -> ApiException.conflict("NO_SHIFT", "Ca bán hàng chưa mở hoặc đã đóng"));

        List<PricedLine> lines = price(req.lines(), storeId);
        long total = lines.stream().mapToLong(PricedLine::amount).sum();
        long redeemable = redeemableAmount(lines);

        CustomerService.Customer member = null;
        if (req.customerId() != null) {
            member = customers.findById(req.customerId()).filter(c -> !"MERGED".equals(c.status()))
                    .orElseThrow(() -> ApiException.badRequest("MEMBER", "Thành viên không hợp lệ"));
        }
        if (req.redemptionId() != null && member == null)
            throw ApiException.badRequest("MEMBER", "Đổi điểm cần gắn thành viên vào hóa đơn");

        OffsetDateTime now = OffsetDateTime.now(Texts.VN);
        long txnId = jdbc.sql("""
                INSERT INTO pos.sales_transactions (receipt_no, store_id, terminal_id, shift_id, cashier_id, customer_id, txn_type,
                    status, source, txn_time, synced_at, net_amount, discount_amount)
                VALUES (:r, :store, :term, :shift, :cashier, :cust, 'SALE', 'COMPLETED', 'POS', :now, :now, :total, 0)
                RETURNING transaction_id
                """).param("r", req.receiptNo()).param("store", storeId).param("term", req.terminalId())
                .param("shift", ((Number) shift.get("shift_id")).longValue()).param("cashier", cashier.id())
                .param("cust", member == null ? null : member.customerId()).param("now", now).param("total", total)
                .query(Long.class).single();
        for (PricedLine l : lines) {
            jdbc.sql("""
                    INSERT INTO pos.transaction_lines (transaction_id, product_id, quantity, unit_price, discount, line_amount, point_eligible)
                    VALUES (:t, :p, :q, :price, 0, :amount, :elig)
                    """).param("t", txnId).param("p", l.product().productId()).param("q", l.quantity()).param("price", l.product().price())
                    .param("amount", l.amount()).param("elig", l.product().pointEligible()).update();
        }

        // Đổi điểm: xác nhận phần điểm đang giữ (HELD → CONFIRMED)
        int pointsRedeemed = 0;
        long pointsAmount = 0;
        PointCalculator.Policy policy = config.currentPolicy();
        if (req.redemptionId() != null) {
            RedemptionService.Redemption r = redemptions.confirm(req.redemptionId(), member.customerId(), req.terminalId(), txnId, cashier.id());
            if (r.amountVnd() > redeemable * policy.maxRedeemPct() / 100)
                throw ApiException.badRequest("REDEEM_LIMIT", "Số tiền trừ bằng điểm vượt " + policy.maxRedeemPct() + "% giá trị hàng được đổi điểm, vui lòng đổi lại");
            pointsRedeemed = r.points();
            pointsAmount = r.amountVnd();
            jdbc.sql("INSERT INTO pos.payments (transaction_id, method, amount, redemption_id) VALUES (:t, 'POINTS', :a, :r)")
                    .param("t", txnId).param("a", pointsAmount).param("r", r.redemptionId()).update();
        }
        long cashDue = total - pointsAmount;
        if (req.cashTendered() < cashDue) throw ApiException.badRequest("CASH", "Tiền khách đưa chưa đủ");
        if (cashDue > 0)
            jdbc.sql("INSERT INTO pos.payments (transaction_id, method, amount, reference) VALUES (:t, 'CASH', :a, :ref)")
                    .param("t", txnId).param("a", cashDue).param("ref", "tendered=" + req.cashTendered()).update();

        // Tích điểm theo hạng tại thời điểm giao dịch (BRL-03, BRL-04, BRL-06)
        long eligible = Math.max(0, redeemable - pointsAmount);
        int earned = 0;
        String upgradedTo = null;
        if (member != null) {
            PointCalculator.Tier tier = config.tier(member.tierCode());
            earned = PointCalculator.earnedPoints(eligible, policy, tier.multiplier(), BigDecimal.ONE);
            ledger.earn(member.customerId(), txnId, earned, now, policy, cashier.id());
            PointCalculator.Tier after = tiers.afterSale(member.customerId(), total);
            if (!after.code().equals(tier.code())) upgradedTo = after.name();
        }
        jdbc.sql("UPDATE pos.sales_transactions SET eligible_amount = :e, points_earned = :pe, points_redeemed = :pr WHERE transaction_id = :t")
                .param("e", member == null ? null : eligible).param("pe", member == null ? null : earned)
                .param("pr", member == null ? null : pointsRedeemed).param("t", txnId).update();

        Map<String, Object> res = receipt(txnId, req.cashTendered());
        res.put("upgradedTo", upgradedTo);
        return res;
    }

    /** Dữ liệu in hóa đơn (FR-05 ở mức màn hình xem trước): dòng hàng, thanh toán, điểm cộng, số dư, hạng. */
    public Map<String, Object> receipt(long txnId, Long cashTendered) {
        Map<String, Object> t = jdbc.sql("""
                SELECT t.transaction_id, t.receipt_no, t.txn_time, t.net_amount, t.eligible_amount, t.points_earned, t.points_redeemed,
                       s.store_code, s.name AS store_name, s.address AS store_address, pt.terminal_code, u.full_name AS cashier_name,
                       c.member_code, c.full_name AS member_name, c.phone, c.point_balance, tr.name AS tier_name
                FROM pos.sales_transactions t JOIN admin.stores s ON s.store_id = t.store_id
                LEFT JOIN admin.pos_terminals pt ON pt.terminal_id = t.terminal_id
                LEFT JOIN admin.app_users u ON u.user_id = t.cashier_id
                LEFT JOIN customer.customers c ON c.customer_id = t.customer_id
                LEFT JOIN loyalty.tiers tr ON tr.tier_code = c.tier_code
                WHERE t.transaction_id = :id
                """).param("id", txnId).query().singleRow();
        Object phone = t.remove("phone");
        if (phone != null) t.put("memberPhone", Texts.maskPhone((String) phone));
        t.put("lines", jdbc.sql("""
                SELECT p.sku, p.name, p.unit, l.quantity, l.unit_price, l.line_amount, l.point_eligible
                FROM pos.transaction_lines l JOIN catalog.products p ON p.product_id = l.product_id
                WHERE l.transaction_id = :id ORDER BY l.line_id
                """).param("id", txnId).query().listOfRows());
        List<Map<String, Object>> payments = jdbc.sql("SELECT method, amount FROM pos.payments WHERE transaction_id = :id ORDER BY payment_id")
                .param("id", txnId).query().listOfRows();
        t.put("payments", payments);
        long cash = payments.stream().filter(p -> "CASH".equals(p.get("method"))).mapToLong(p -> ((Number) p.get("amount")).longValue()).sum();
        t.put("cashTendered", cashTendered);
        t.put("change", cashTendered == null ? null : cashTendered - cash);
        return new LinkedHashMap<>(t);
    }
}
