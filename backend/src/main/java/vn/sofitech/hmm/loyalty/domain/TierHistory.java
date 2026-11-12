package vn.sofitech.hmm.loyalty.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Lịch sử thay đổi hạng (loyalty.tier_histories). */
@Entity
@Table(schema = "loyalty", name = "tier_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TierHistory {

    public enum Reason { UPGRADE, DOWNGRADE, MIGRATION }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tier_history_seq")
    @SequenceGenerator(name = "tier_history_seq", schema = "loyalty", sequenceName = "tier_histories_history_id_seq", allocationSize = 50)
    @Column(name = "history_id")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "from_tier")
    private String fromTier;

    @Column(name = "to_tier", nullable = false)
    private String toTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    public TierHistory(Long customerId, String fromTier, String toTier, Reason reason, OffsetDateTime changedAt) {
        this.customerId = customerId;
        this.fromTier = fromTier;
        this.toTier = toTier;
        this.reason = reason;
        this.changedAt = changedAt;
    }
}
