package vn.sofitech.hmm.report;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static vn.sofitech.hmm.shared.Texts.VN;

/** Dashboard KPI (FR-51): số thành viên, tỷ lệ định danh, tỷ trọng doanh thu thành viên, tỷ lệ quay lại, phân bố hạng. */
@Service
public class DashboardService {

    private final JdbcClient jdbc;

    public DashboardService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> dashboard(LocalDate from, LocalDate to, Long storeId) {
        OffsetDateTime start = from.atStartOfDay(VN).toOffsetDateTime();
        OffsetDateTime end = to.plusDays(1).atStartOfDay(VN).toOffsetDateTime();
        String txnFilter = """
                t.txn_type = 'SALE' AND t.status = 'COMPLETED' AND t.txn_time >= :start AND t.txn_time < :end
                AND (CAST(:store AS bigint) IS NULL OR t.store_id = :store)
                """;

        Map<String, Object> kpi = new LinkedHashMap<>(jdbc.sql("""
                SELECT count(*) AS receipts,
                       COALESCE(sum(t.net_amount), 0) AS revenue,
                       count(t.customer_id) AS member_receipts,
                       COALESCE(sum(t.net_amount) FILTER (WHERE t.customer_id IS NOT NULL), 0) AS member_revenue,
                       count(DISTINCT t.customer_id) AS active_members,
                       COALESCE(sum(t.points_earned), 0) AS points_earned,
                       COALESCE(sum(t.points_redeemed), 0) AS points_redeemed
                FROM pos.sales_transactions t WHERE
                """ + txnFilter).param("start", start).param("end", end).param("store", storeId).query().singleRow());
        long receipts = ((Number) kpi.get("receipts")).longValue();
        long revenue = ((Number) kpi.get("revenue")).longValue();
        kpi.put("identificationRate", receipts == 0 ? 0 : round1(100.0 * ((Number) kpi.get("member_receipts")).longValue() / receipts));
        kpi.put("memberRevenueShare", revenue == 0 ? 0 : round1(100.0 * ((Number) kpi.get("member_revenue")).longValue() / revenue));
        kpi.put("avgBasket", receipts == 0 ? 0 : revenue / receipts);

        // Tỷ lệ quay lại: thành viên mua ≥ 2 lần trong kỳ / thành viên có mua trong kỳ
        Map<String, Object> ret = jdbc.sql("""
                SELECT count(*) AS buyers, count(*) FILTER (WHERE n >= 2) AS returning
                FROM (SELECT t.customer_id, count(*) AS n FROM pos.sales_transactions t
                      WHERE t.customer_id IS NOT NULL AND
                """ + txnFilter + " GROUP BY t.customer_id) x").param("start", start).param("end", end).param("store", storeId).query().singleRow();
        long buyers = ((Number) ret.get("buyers")).longValue();
        kpi.put("returningRate", buyers == 0 ? 0 : round1(100.0 * ((Number) ret.get("returning")).longValue() / buyers));

        Map<String, Object> members = jdbc.sql("""
                SELECT count(*) AS total_members,
                       count(*) FILTER (WHERE c.created_at >= :start AND c.created_at < :end) AS new_members,
                       count(*) FILTER (WHERE c.status = 'VERIFIED') AS verified_members
                FROM customer.customers c WHERE c.status <> 'MERGED' AND (CAST(:store AS bigint) IS NULL OR c.registered_store_id = :store)
                """).param("start", start).param("end", end).param("store", storeId).query().singleRow();
        kpi.putAll(members);

        List<Map<String, Object>> tiers = jdbc.sql("""
                SELECT tr.tier_code, tr.name, count(c.customer_id) AS members
                FROM loyalty.tiers tr LEFT JOIN customer.customers c ON c.tier_code = tr.tier_code AND c.status <> 'MERGED'
                     AND (CAST(:store AS bigint) IS NULL OR c.registered_store_id = :store)
                GROUP BY tr.tier_code, tr.name, tr.threshold_spend ORDER BY tr.threshold_spend
                """).param("store", storeId).query().listOfRows();

        List<Map<String, Object>> byStore = jdbc.sql("""
                SELECT s.store_code, s.name, count(t.transaction_id) AS receipts, COALESCE(sum(t.net_amount), 0) AS revenue,
                       COALESCE(sum(t.net_amount) FILTER (WHERE t.customer_id IS NOT NULL), 0) AS member_revenue,
                       count(t.customer_id) AS member_receipts
                FROM admin.stores s LEFT JOIN pos.sales_transactions t ON t.store_id = s.store_id AND
                """ + txnFilter + """
                WHERE (CAST(:store AS bigint) IS NULL OR s.store_id = :store)
                GROUP BY s.store_code, s.name ORDER BY s.store_code
                """).param("start", start).param("end", end).param("store", storeId).query().listOfRows();

        List<Map<String, Object>> byDay = jdbc.sql("""
                SELECT to_char(t.txn_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'YYYY-MM-DD') AS day,
                       COALESCE(sum(t.net_amount) FILTER (WHERE t.customer_id IS NOT NULL), 0) AS member_revenue,
                       COALESCE(sum(t.net_amount) FILTER (WHERE t.customer_id IS NULL), 0) AS guest_revenue,
                       count(*) AS receipts, count(t.customer_id) AS member_receipts
                FROM pos.sales_transactions t WHERE
                """ + txnFilter + " GROUP BY 1 ORDER BY 1").param("start", start).param("end", end).param("store", storeId).query().listOfRows();

        List<Map<String, Object>> categories = jdbc.sql("""
                SELECT c.name, COALESCE(sum(l.line_amount), 0) AS revenue
                FROM pos.transaction_lines l JOIN pos.sales_transactions t ON t.transaction_id = l.transaction_id
                JOIN catalog.products p ON p.product_id = l.product_id JOIN catalog.categories c ON c.category_id = p.category_id
                WHERE
                """ + txnFilter + " GROUP BY c.name ORDER BY 2 DESC LIMIT 8").param("start", start).param("end", end).param("store", storeId).query().listOfRows();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("from", from);
        res.put("to", to);
        res.put("storeId", storeId);
        res.put("kpi", kpi);
        res.put("tiers", tiers);
        res.put("byStore", byStore);
        res.put("byDay", byDay);
        res.put("categories", categories);
        return res;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
