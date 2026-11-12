package vn.sofitech.hmm.loyalty.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Chính sách điểm có phiên bản và ngày hiệu lực (loyalty.loyalty_policies). */
@Entity
@Table(schema = "loyalty", name = "loyalty_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoyaltyPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private int version;

    @Column(name = "earn_unit_vnd", nullable = false)
    private int earnUnitVnd;

    @Column(name = "point_value_vnd", nullable = false)
    private int pointValueVnd;

    @Column(name = "min_redeem_points", nullable = false)
    private int minRedeemPoints;

    @Column(name = "max_redeem_pct", nullable = false)
    private int maxRedeemPct;

    @Column(name = "expiry_months", nullable = false)
    private int expiryMonths;

    @Column(name = "hold_minutes", nullable = false)
    private int holdMinutes;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(nullable = false)
    private String status;

    @Column(name = "approved_by")
    private Long approvedBy;

    public LoyaltyPolicy(int version, int earnUnitVnd, int pointValueVnd, int minRedeemPoints, int maxRedeemPct,
                         int expiryMonths, int holdMinutes, OffsetDateTime effectiveFrom, Long approvedBy) {
        this.version = version;
        this.earnUnitVnd = earnUnitVnd;
        this.pointValueVnd = pointValueVnd;
        this.minRedeemPoints = minRedeemPoints;
        this.maxRedeemPct = maxRedeemPct;
        this.expiryMonths = expiryMonths;
        this.holdMinutes = holdMinutes;
        this.effectiveFrom = effectiveFrom;
        this.status = "APPROVED";
        this.approvedBy = approvedBy;
    }
}
