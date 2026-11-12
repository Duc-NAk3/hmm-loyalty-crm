package vn.sofitech.hmm.loyalty.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

/**
 * Bút toán sổ cái điểm (loyalty.point_ledgers) – bất biến, chỉ ghi thêm (FR-33).
 * {@code @Immutable}: Hibernate không bao giờ sinh UPDATE; CSDL có thêm trigger chặn UPDATE/DELETE.
 */
@Entity
@Immutable
@Table(schema = "loyalty", name = "point_ledgers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLedger {

    public enum EntryType { EARN, REDEEM, HOLD, RELEASE, EXPIRE, ADJUST, REVERSE, MERGE }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ledger_seq")
    @SequenceGenerator(name = "ledger_seq", schema = "loyalty", sequenceName = "point_ledgers_ledger_id_seq", allocationSize = 50)
    @Column(name = "ledger_id")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    /** Dương là cộng, âm là trừ. */
    @Column(nullable = false)
    private int points;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private PointLot lot;

    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id")
    private PointRedemption redemption;

    @Column(name = "adjustment_id")
    private Long adjustmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id")
    private LoyaltyPolicy policy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public PointLedger(Long customerId, EntryType entryType, int points, int balanceAfter, PointLot lot, Long transactionId,
                       PointRedemption redemption, LoyaltyPolicy policy, Long createdBy, OffsetDateTime createdAt) {
        this.customerId = customerId;
        this.entryType = entryType;
        this.points = points;
        this.balanceAfter = balanceAfter;
        this.lot = lot;
        this.transactionId = transactionId;
        this.redemption = redemption;
        this.policy = policy;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
