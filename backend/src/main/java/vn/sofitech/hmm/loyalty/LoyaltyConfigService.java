package vn.sofitech.hmm.loyalty;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import vn.sofitech.hmm.shared.ApiException;

import java.util.List;
import java.util.Map;

/** Đọc chính sách điểm đang hiệu lực và danh sách hạng. */
@Service
public class LoyaltyConfigService {

    private final JdbcClient jdbc;

    public LoyaltyConfigService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public PointCalculator.Policy currentPolicy() {
        return jdbc.sql("""
                SELECT policy_id, earn_unit_vnd, point_value_vnd, min_redeem_points, max_redeem_pct, expiry_months, hold_minutes
                FROM loyalty.loyalty_policies WHERE status = 'APPROVED' AND effective_from <= now()
                ORDER BY effective_from DESC, version DESC LIMIT 1
                """).query((rs, i) -> new PointCalculator.Policy(rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                rs.getInt(5), rs.getInt(6), rs.getInt(7))).optional()
                .orElseThrow(() -> ApiException.conflict("NO_POLICY", "Chưa có chính sách điểm đang hiệu lực"));
    }

    public List<PointCalculator.Tier> tiers() {
        return jdbc.sql("SELECT tier_code, name, threshold_spend, multiplier FROM loyalty.tiers ORDER BY threshold_spend")
                .query((rs, i) -> new PointCalculator.Tier(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getBigDecimal(4))).list();
    }

    public PointCalculator.Tier tier(String code) {
        return tiers().stream().filter(t -> t.code().equals(code)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hạng " + code));
    }

    public List<Map<String, Object>> tierTable() {
        return jdbc.sql("""
                SELECT t.tier_code, t.name, t.threshold_spend, t.multiplier, t.benefits,
                       (SELECT count(*) FROM customer.customers c WHERE c.tier_code = t.tier_code AND c.status <> 'MERGED') AS members
                FROM loyalty.tiers t ORDER BY t.threshold_spend
                """).query().listOfRows();
    }
}
