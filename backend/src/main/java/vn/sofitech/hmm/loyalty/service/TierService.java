package vn.sofitech.hmm.loyalty.service;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.customer.api.CustomerInfo;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.loyalty.domain.TierHistory;
import vn.sofitech.hmm.loyalty.repository.TierHistoryRepository;
import vn.sofitech.hmm.shared.SmsRequestedEvent;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hạng thành viên (FR-31, BRL-11): thăng hạng ngay khi chi tiêu 12 tháng đạt ngưỡng; xét hạ hạng hằng ngày
 * theo ngày kỷ niệm lên hạng (hạng giữ tối thiểu 12 tháng, hạ 1 bậc nếu không đạt ngưỡng).
 */
@Service
public class TierService {

    /** Kết quả xét hạng. */
    public record ReviewResult(int customersChecked, int spendRecalculated, int upgraded, int downgraded) {
    }

    private static final Logger log = LoggerFactory.getLogger(TierService.class);

    private final CustomerFacade customers;
    private final LoyaltyConfigService config;
    private final TierHistoryRepository histories;
    private final ApplicationEventPublisher events;
    private final EntityManager em;

    public TierService(CustomerFacade customers, LoyaltyConfigService config, TierHistoryRepository histories,
                       ApplicationEventPublisher events, EntityManager em) {
        this.customers = customers;
        this.config = config;
        this.histories = histories;
        this.events = events;
        this.em = em;
    }

    /** Sau mỗi hóa đơn: cộng chi tiêu 12 tháng và thăng hạng ngay nếu đạt ngưỡng. Trả về hạng sau xử lý. */
    @Transactional(propagation = Propagation.MANDATORY)
    public PointCalculator.Tier afterSale(long customerId, long amount) {
        CustomerInfo c = customers.addSpend(customerId, amount);
        List<PointCalculator.Tier> tiers = config.tiers();
        PointCalculator.Tier current = find(tiers, c.getTierCode());
        PointCalculator.Tier target = PointCalculator.tierFor(c.getSpend12m(), tiers);
        if (target.thresholdSpend() <= current.thresholdSpend()) return current;
        changeTier(customerId, current.code(), target.code(), TierHistory.Reason.UPGRADE, OffsetDateTime.now(Texts.VN));
        events.publishEvent(new SmsRequestedEvent("SMS_TIER", "customers", customerId, c.getPhone(),
                "Chuc mung ban da len hang " + Texts.unaccent(target.name()).toUpperCase() + " tai Hoa Mai Mart! He so tich diem moi: x" + target.multiplier()));
        return target;
    }

    private void changeTier(long customerId, String from, String to, TierHistory.Reason reason, OffsetDateTime at) {
        customers.changeTier(customerId, to);
        histories.save(new TierHistory(customerId, from, to, reason, at));
    }

    /** Tác vụ hằng ngày 02:00 (và nút "Chạy xét hạng" trên Portal để demo). */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public ReviewResult dailyReview() {
        OffsetDateTime now = OffsetDateTime.now(Texts.VN);
        // 1. Chi tiêu 12 tháng từ hóa đơn thực tế. Truy vấn đọc (JPQL theo tên entity) – không phụ thuộc kiểu của module pos.
        Map<Long, Long> spend = new HashMap<>();
        em.createQuery("""
                select t.customerId,
                       sum(case when t.txnType = vn.sofitech.hmm.pos.domain.SalesTransaction.Type.RETURN then -t.netAmount else t.netAmount end)
                from SalesTransaction t
                where t.customerId is not null and t.status = vn.sofitech.hmm.pos.domain.SalesTransaction.Status.COMPLETED
                  and t.txnTime >= :since
                group by t.customerId
                """, Object[].class).setParameter("since", now.minusMonths(12)).getResultList()
                .forEach(row -> spend.put((Long) row[0], ((Number) row[1]).longValue()));
        Map<Long, OffsetDateTime> lastChange = new HashMap<>();
        histories.lastChangePerCustomer().forEach(row -> lastChange.put((Long) row[0], (OffsetDateTime) row[1]));

        // 2. Thăng hoặc hạ hạng
        List<PointCalculator.Tier> tiers = config.tiers();
        List<CustomerInfo> all = customers.findAllForTierReview();
        int recalculated = 0, up = 0, down = 0;
        for (CustomerInfo c : all) {
            long s = spend.getOrDefault(c.getCustomerId(), 0L);
            if (s != c.getSpend12m()) {
                customers.setSpend12m(c.getCustomerId(), s);
                recalculated++;
            }
            PointCalculator.Tier current = find(tiers, c.getTierCode());
            PointCalculator.Tier target = PointCalculator.tierFor(s, tiers);
            OffsetDateTime since = lastChange.getOrDefault(c.getCustomerId(), c.getCreatedAt());
            if (target.thresholdSpend() > current.thresholdSpend()) {
                changeTier(c.getCustomerId(), current.code(), target.code(), TierHistory.Reason.UPGRADE, now);
                up++;
            } else if (target.thresholdSpend() < current.thresholdSpend() && !since.plusMonths(12).isAfter(now)) {
                changeTier(c.getCustomerId(), current.code(), PointCalculator.oneLevelDown(current, tiers).code(), TierHistory.Reason.DOWNGRADE, now);
                down++;
            }
        }
        log.info("Tier review: spend recalculated {}, upgraded {}, downgraded {}", recalculated, up, down);
        return new ReviewResult(all.size(), recalculated, up, down);
    }

    private static PointCalculator.Tier find(List<PointCalculator.Tier> tiers, String code) {
        return tiers.stream().filter(t -> t.code().equals(code)).findFirst().orElseThrow();
    }
}
