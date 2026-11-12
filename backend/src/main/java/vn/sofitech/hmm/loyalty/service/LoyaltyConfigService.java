package vn.sofitech.hmm.loyalty.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.loyalty.domain.LoyaltyPolicy;
import vn.sofitech.hmm.loyalty.domain.LoyaltyPolicyRepository;
import vn.sofitech.hmm.loyalty.domain.TierRepository;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;
import java.util.List;

/** Đọc chính sách điểm đang hiệu lực và danh sách hạng. */
@Service
@Transactional(readOnly = true)
public class LoyaltyConfigService {

    private final LoyaltyPolicyRepository policies;
    private final TierRepository tiers;

    public LoyaltyConfigService(LoyaltyPolicyRepository policies, TierRepository tiers) {
        this.policies = policies;
        this.tiers = tiers;
    }

    public LoyaltyPolicy currentPolicyEntity() {
        return policies.findFirstByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescVersionDesc("APPROVED", OffsetDateTime.now(Texts.VN))
                .orElseThrow(() -> ApiException.conflict("NO_POLICY", "Chưa có chính sách điểm đang hiệu lực"));
    }

    public PointCalculator.Policy currentPolicy() {
        LoyaltyPolicy p = currentPolicyEntity();
        return new PointCalculator.Policy(p.getId(), p.getEarnUnitVnd(), p.getPointValueVnd(), p.getMinRedeemPoints(),
                p.getMaxRedeemPct(), p.getExpiryMonths(), p.getHoldMinutes());
    }

    public List<PointCalculator.Tier> tiers() {
        return tiers.findAllByOrderByThresholdSpend().stream()
                .map(t -> new PointCalculator.Tier(t.getCode(), t.getName(), t.getThresholdSpend(), t.getMultiplier())).toList();
    }

    public PointCalculator.Tier tier(String code) {
        return tiers().stream().filter(t -> t.code().equals(code)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hạng " + code));
    }

    public List<vn.sofitech.hmm.loyalty.domain.Tier> tierEntities() {
        return tiers.findAllByOrderByThresholdSpend();
    }
}
