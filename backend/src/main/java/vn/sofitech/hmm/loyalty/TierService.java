package vn.sofitech.hmm.loyalty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.shared.OutboxService;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Hạng thành viên (FR-31, BRL-11): thăng hạng ngay khi chi tiêu 12 tháng đạt ngưỡng; xét hạ hạng hằng ngày
 * theo ngày kỷ niệm lên hạng (hạng giữ tối thiểu 12 tháng, hạ 1 bậc nếu không đạt ngưỡng).
 */
@Service
public class TierService {

    private static final Logger log = LoggerFactory.getLogger(TierService.class);

    private final JdbcClient jdbc;
    private final LoyaltyConfigService config;
    private final OutboxService outbox;

    public TierService(JdbcClient jdbc, LoyaltyConfigService config, OutboxService outbox) {
        this.jdbc = jdbc;
        this.config = config;
        this.outbox = outbox;
    }

    /** Sau mỗi hóa đơn: cộng chi tiêu 12 tháng và thăng hạng ngay nếu đạt ngưỡng. Trả về hạng sau xử lý. */
    @Transactional(propagation = Propagation.MANDATORY)
    public PointCalculator.Tier afterSale(long customerId, long amount) {
        record Row(String tier, long spend, String phone) {
        }
        Row r = jdbc.sql("""
                UPDATE customer.customers SET spend_12m = spend_12m + :a, updated_at = now() WHERE customer_id = :c
                RETURNING tier_code, spend_12m, phone
                """).param("a", amount).param("c", customerId).query((rs, i) -> new Row(rs.getString(1), rs.getLong(2), rs.getString(3))).single();
        List<PointCalculator.Tier> tiers = config.tiers();
        PointCalculator.Tier current = tiers.stream().filter(t -> t.code().equals(r.tier())).findFirst().orElseThrow();
        PointCalculator.Tier target = PointCalculator.tierFor(r.spend(), tiers);
        if (target.thresholdSpend() > current.thresholdSpend()) {
            changeTier(customerId, current.code(), target.code(), "UPGRADE", OffsetDateTime.now(Texts.VN));
            outbox.enqueueSms("SMS_TIER", "customers", customerId, r.phone(),
                    "Chuc mung ban da len hang " + Texts.unaccent(target.name()).toUpperCase() + " tai Hoa Mai Mart! He so tich diem moi: x" + target.multiplier());
            return target;
        }
        return current;
    }

    public void changeTier(long customerId, String from, String to, String reason, OffsetDateTime at) {
        jdbc.sql("UPDATE customer.customers SET tier_code = :to, updated_at = now() WHERE customer_id = :c").param("to", to).param("c", customerId).update();
        jdbc.sql("INSERT INTO loyalty.tier_histories (customer_id, from_tier, to_tier, reason, changed_at) VALUES (:c, :f, :t, :r, :at)")
                .param("c", customerId).param("f", from).param("t", to).param("r", reason).param("at", at).update();
    }

    /** Tác vụ hằng ngày 02:00 (và nút "Chạy xét hạng" trên Portal để demo). */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public Map<String, Object> dailyReview() {
        // 1. Tính lại chi tiêu 12 tháng từ giao dịch thực tế
        int recalculated = jdbc.sql("""
                UPDATE customer.customers c SET spend_12m = COALESCE(s.total, 0), updated_at = now()
                FROM (SELECT cu.customer_id,
                             (SELECT sum(CASE WHEN t.txn_type = 'RETURN' THEN -t.net_amount ELSE t.net_amount END)
                              FROM pos.sales_transactions t
                              WHERE t.customer_id = cu.customer_id AND t.status = 'COMPLETED' AND t.txn_time >= now() - interval '12 months') AS total
                      FROM customer.customers cu WHERE cu.status <> 'MERGED') s
                WHERE c.customer_id = s.customer_id AND c.spend_12m IS DISTINCT FROM COALESCE(s.total, 0)
                """).update();
        // 2. Thăng hoặc hạ hạng
        List<PointCalculator.Tier> tiers = config.tiers();
        record Cand(long id, String tier, long spend, OffsetDateTime since) {
        }
        List<Cand> cands = jdbc.sql("""
                SELECT c.customer_id, c.tier_code, c.spend_12m,
                       COALESCE((SELECT max(h.changed_at) FROM loyalty.tier_histories h WHERE h.customer_id = c.customer_id), c.created_at) AS since
                FROM customer.customers c WHERE c.status NOT IN ('MERGED', 'ANONYMIZED')
                """).query((rs, i) -> new Cand(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getObject(4, OffsetDateTime.class))).list();
        int up = 0, down = 0;
        OffsetDateTime now = OffsetDateTime.now(Texts.VN);
        for (Cand c : cands) {
            PointCalculator.Tier current = tiers.stream().filter(t -> t.code().equals(c.tier())).findFirst().orElseThrow();
            PointCalculator.Tier target = PointCalculator.tierFor(c.spend(), tiers);
            if (target.thresholdSpend() > current.thresholdSpend()) {
                changeTier(c.id(), current.code(), target.code(), "UPGRADE", now);
                up++;
            } else if (target.thresholdSpend() < current.thresholdSpend() && !c.since().plusMonths(12).isAfter(now)) {
                changeTier(c.id(), current.code(), PointCalculator.oneLevelDown(current, tiers).code(), "DOWNGRADE", now);
                down++;
            }
        }
        log.info("Tier review: spend recalculated {}, upgraded {}, downgraded {}", recalculated, up, down);
        return Map.of("customersChecked", cands.size(), "spendRecalculated", recalculated, "upgraded", up, "downgraded", down);
    }
}
