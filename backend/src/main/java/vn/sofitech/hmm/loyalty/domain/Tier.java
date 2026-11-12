package vn.sofitech.hmm.loyalty.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Hạng thành viên (loyalty.tiers): MEMBER / SILVER / GOLD / DIAMOND. */
@Entity
@Table(schema = "loyalty", name = "tiers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tier {

    @Id
    @Column(name = "tier_code")
    private String code;

    @Column(nullable = false)
    private String name;

    /** Ngưỡng chi tiêu 12 tháng (VNĐ). */
    @Column(name = "threshold_spend", nullable = false)
    private long thresholdSpend;

    /** Hệ số tích điểm (BRL-04). */
    @Column(nullable = false)
    private BigDecimal multiplier;

    private String benefits;

    public Tier(String code, String name, long thresholdSpend, BigDecimal multiplier, String benefits) {
        this.code = code;
        this.name = name;
        this.thresholdSpend = thresholdSpend;
        this.multiplier = multiplier;
        this.benefits = benefits;
    }
}
