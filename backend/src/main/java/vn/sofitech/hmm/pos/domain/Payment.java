package vn.sofitech.hmm.pos.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Khoản thanh toán của hóa đơn (pos.payments). Một hóa đơn có thể trả bằng nhiều phương thức. */
@Entity
@Table(schema = "pos", name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    public enum Method { CASH, CARD, BANK_QR, POINTS, VOUCHER }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_seq")
    @SequenceGenerator(name = "payment_seq", schema = "pos", sequenceName = "payments_payment_id_seq", allocationSize = 50)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private SalesTransaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method;

    @Column(nullable = false)
    private long amount;

    /** Lần đổi điểm (module loyalty) khi method = POINTS – chỉ giữ ID. */
    @Column(name = "redemption_id")
    private Long redemptionId;

    @Column(name = "voucher_id")
    private Long voucherId;

    private String reference;

    Payment(SalesTransaction transaction, Method method, long amount, Long redemptionId, String reference) {
        this.transaction = transaction;
        this.method = method;
        this.amount = amount;
        this.redemptionId = redemptionId;
        this.reference = reference;
    }
}
