package vn.sofitech.hmm.loyalty.repository;

import vn.sofitech.hmm.loyalty.domain.LoyaltyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface LoyaltyPolicyRepository extends JpaRepository<LoyaltyPolicy, Long> {

    /** Chính sách đã duyệt có hiệu lực mới nhất tại thời điểm {@code at}. */
    Optional<LoyaltyPolicy> findFirstByStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescVersionDesc(String status, OffsetDateTime at);
}
