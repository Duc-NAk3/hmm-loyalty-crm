package vn.sofitech.hmm.loyalty.service;

import vn.sofitech.hmm.loyalty.dto.member.MemberSummaryDto;
import vn.sofitech.hmm.loyalty.dto.tier.TierDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.customer.api.CustomerInfo;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.loyalty.repository.PointLotRepository;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Thông tin thành viên tại quầy: hạng, điểm khả dụng, điểm sắp hết hạn, tiến độ lên hạng (FR-03, FR-18). */
@Service
@Transactional(readOnly = true)
public class MemberService {

    private final CustomerFacade customers;
    private final LoyaltyConfigService config;
    private final PointLotRepository lots;

    public MemberService(CustomerFacade customers, LoyaltyConfigService config, PointLotRepository lots) {
        this.customers = customers;
        this.config = config;
        this.lots = lots;
    }

    public MemberSummaryDto summary(CustomerInfo c) {
        List<PointCalculator.Tier> tiers = config.tiers();
        PointCalculator.Tier tier = tiers.stream().filter(t -> t.code().equals(c.getTierCode())).findFirst().orElseThrow();
        PointCalculator.Tier next = tiers.stream().filter(t -> t.thresholdSpend() > tier.thresholdSpend()).findFirst().orElse(null);
        OffsetDateTime now = OffsetDateTime.now(Texts.VN);
        long expiring = lots.sumExpiring(c.getCustomerId(), now, now.plusDays(30));
        OffsetDateTime expiringAt = expiring > 0 ? lots.firstExpiry(c.getCustomerId(), now, now.plusDays(30)) : null;
        return new MemberSummaryDto(c.getCustomerId(), c.getMemberCode(), c.getFullName(), Texts.maskPhone(c.getPhone()), c.getStatus(),
                "VERIFIED".equals(c.getStatus()), c.isRedeemLocked(), tier.code(), tier.name(), tier.multiplier(), c.getPointBalance(),
                (long) c.getPointBalance() * config.currentPolicy().pointValueVnd(), expiring, expiringAt, c.getSpend12m(),
                next == null ? null : next.name(), next == null ? 0 : Math.max(0, next.thresholdSpend() - c.getSpend12m()));
    }

    public List<TierDto> tierTable() {
        Map<String, Long> counts = customers.countByTier();
        return config.tierEntities().stream().map(t -> new TierDto(t.getCode(), t.getName(), t.getThresholdSpend(), t.getMultiplier(),
                t.getBenefits(), counts.getOrDefault(t.getCode(), 0L))).toList();
    }
}
