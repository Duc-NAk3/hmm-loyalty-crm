package vn.sofitech.hmm.report;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;
import vn.sofitech.hmm.shared.Texts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer 360 (FR-22): hồ sơ, hạng và tiến độ lên hạng, số dư và sổ cái điểm, lịch sử giao dịch đến từng dòng hàng
 * ở mọi cửa hàng, consent, lịch sử hạng, nguồn dữ liệu chuyển đổi. SĐT đầy đủ chỉ hiện khi bấm xem và được ghi audit.
 */
@Service
public class Customer360Service {

    private final JdbcClient jdbc;
    private final AuditService audit;

    public Customer360Service(JdbcClient jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public Map<String, Object> view(long customerId) {
        Map<String, Object> p = jdbc.sql("""
                SELECT c.customer_id, c.member_code, c.full_name, c.phone, c.gender, c.date_of_birth, c.email, c.status, c.verified_at,
                       c.tier_code, t.name AS tier_name, t.multiplier, c.point_balance, c.spend_12m, c.redeem_locked, c.is_employee,
                       s.store_code AS registered_store, s.name AS registered_store_name, c.created_at, c.merged_into_id,
                       (SELECT n.name FROM loyalty.tiers n WHERE n.threshold_spend > t.threshold_spend ORDER BY n.threshold_spend LIMIT 1) AS next_tier,
                       (SELECT n.threshold_spend FROM loyalty.tiers n WHERE n.threshold_spend > t.threshold_spend ORDER BY n.threshold_spend LIMIT 1) AS next_threshold,
                       t.threshold_spend
                FROM customer.customers c JOIN loyalty.tiers t ON t.tier_code = c.tier_code
                LEFT JOIN admin.stores s ON s.store_id = c.registered_store_id
                WHERE c.customer_id = :id
                """).param("id", customerId).query().listOfRows().stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng"));
        p.put("phone", Texts.maskPhone((String) p.get("phone")));

        Map<String, Object> stats = jdbc.sql("""
                SELECT count(*) AS receipts, COALESCE(sum(net_amount), 0) AS lifetime_spend, max(txn_time) AS last_purchase,
                       count(DISTINCT store_id) AS stores_visited,
                       COALESCE((SELECT sum(points_earned) FROM loyalty.point_lots l WHERE l.customer_id = :id), 0) AS points_earned_total,
                       COALESCE((SELECT -sum(points) FROM loyalty.point_ledgers g WHERE g.customer_id = :id AND g.entry_type = 'REDEEM'), 0) AS points_redeemed_total,
                       (SELECT COALESCE(sum(points), 0) FROM loyalty.point_ledgers g WHERE g.customer_id = :id) AS ledger_sum,
                       (SELECT COALESCE(sum(points_remaining), 0) FROM loyalty.point_lots l WHERE l.customer_id = :id AND points_remaining > 0
                          AND expires_at > now() AND expires_at <= now() + interval '30 days') AS expiring_points,
                       (SELECT min(expires_at) FROM loyalty.point_lots l WHERE l.customer_id = :id AND points_remaining > 0 AND expires_at > now()) AS next_expiry
                FROM pos.sales_transactions WHERE customer_id = :id AND status = 'COMPLETED'
                """).param("id", customerId).query().singleRow();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("profile", p);
        res.put("stats", stats);
        res.put("transactions", jdbc.sql("""
                SELECT t.transaction_id, t.receipt_no, t.txn_time, s.store_code, s.name AS store_name, t.txn_type, t.net_amount,
                       t.points_earned, t.points_redeemed, t.source,
                       (SELECT count(*) FROM pos.transaction_lines l WHERE l.transaction_id = t.transaction_id) AS line_count
                FROM pos.sales_transactions t JOIN admin.stores s ON s.store_id = t.store_id
                WHERE t.customer_id = :id ORDER BY t.txn_time DESC LIMIT 100
                """).param("id", customerId).query().listOfRows());
        res.put("ledger", jdbc.sql("""
                SELECT g.ledger_id, g.created_at, g.entry_type, g.points, g.balance_after, t.receipt_no, g.redemption_id, l.expires_at
                FROM loyalty.point_ledgers g LEFT JOIN pos.sales_transactions t ON t.transaction_id = g.transaction_id
                LEFT JOIN loyalty.point_lots l ON l.lot_id = g.lot_id
                WHERE g.customer_id = :id ORDER BY g.ledger_id DESC LIMIT 200
                """).param("id", customerId).query().listOfRows());
        res.put("consents", jdbc.sql("""
                SELECT c.purpose, c.status, c.channel, c.terms_version, c.captured_at, u.full_name AS captured_by
                FROM customer.consents c LEFT JOIN admin.app_users u ON u.user_id = c.captured_by
                WHERE c.customer_id = :id ORDER BY c.captured_at DESC
                """).param("id", customerId).query().listOfRows());
        res.put("tierHistory", jdbc.sql("""
                SELECT h.changed_at, h.from_tier, h.to_tier, h.reason FROM loyalty.tier_histories h
                WHERE h.customer_id = :id ORDER BY h.changed_at DESC
                """).param("id", customerId).query().listOfRows());
        res.put("sources", jdbc.sql("""
                SELECT r.source_vendor, r.source_customer_ref, b.batch_id, b.imported_at, s.store_code
                FROM migration.customer_source_records r JOIN migration.import_batches b ON b.batch_id = r.batch_id
                JOIN admin.stores s ON s.store_id = b.store_id
                WHERE r.customer_id = :id ORDER BY r.source_id
                """).param("id", customerId).query().listOfRows());
        return res;
    }

    public List<Map<String, Object>> transactionLines(long customerId, long transactionId) {
        Integer owner = jdbc.sql("SELECT 1 FROM pos.sales_transactions WHERE transaction_id = :t AND customer_id = :c")
                .param("t", transactionId).param("c", customerId).query(Integer.class).optional().orElse(null);
        if (owner == null) throw ApiException.notFound("Không tìm thấy hóa đơn của khách");
        return jdbc.sql("""
                SELECT p.sku, p.name, c.name AS category, l.quantity, l.unit_price, l.line_amount, l.point_eligible
                FROM pos.transaction_lines l JOIN catalog.products p ON p.product_id = l.product_id
                JOIN catalog.categories c ON c.category_id = p.category_id
                WHERE l.transaction_id = :t ORDER BY l.line_id
                """).param("t", transactionId).query().listOfRows();
    }

    public Map<String, Object> revealPhone(long customerId, long userId) {
        String phone = jdbc.sql("SELECT phone FROM customer.customers WHERE customer_id = :id").param("id", customerId)
                .query(String.class).optional().orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng"));
        audit.log(userId, "VIEW_FULL_PHONE", "customers", customerId, null, null);
        return Map.of("phone", phone);
    }
}
