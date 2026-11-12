package vn.sofitech.hmm.customer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Lịch sử đồng ý / rút đồng ý của khách (customer.consents) – chỉ ghi thêm. */
@Entity
@Table(schema = "customer", name = "consents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consent {

    public enum Purpose { MEMBERSHIP, MARKETING_SMS }

    public enum Status { GRANTED, WITHDRAWN }

    public static final String TERMS_VERSION = "v1.0";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "consent_seq")
    @SequenceGenerator(name = "consent_seq", schema = "customer", sequenceName = "consents_consent_id_seq", allocationSize = 50)
    @Column(name = "consent_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** Nơi khách bày tỏ đồng ý: POS / HOTLINE / SMS / PORTAL. */
    private String channel;

    @Column(name = "terms_version")
    private String termsVersion;

    @Column(name = "captured_by")
    private Long capturedBy;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    public Consent(Customer customer, Purpose purpose, Status status, String channel, Long capturedBy, OffsetDateTime capturedAt) {
        this.customer = customer;
        this.purpose = purpose;
        this.status = status;
        this.channel = channel;
        this.termsVersion = TERMS_VERSION;
        this.capturedBy = capturedBy;
        this.capturedAt = capturedAt;
    }
}
