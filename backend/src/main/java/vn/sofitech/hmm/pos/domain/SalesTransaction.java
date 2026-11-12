package vn.sofitech.hmm.pos.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hóa đơn bán hàng (pos.sales_transactions) – aggregate root gồm các dòng hàng và khoản thanh toán.
 * receipt_no do quầy sinh là khóa chống trùng khi gửi lại.
 */
@Entity
@Table(schema = "pos", name = "sales_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesTransaction {

    public enum Type { SALE, RETURN }

    public enum Status { COMPLETED, VOIDED }

    public enum Source { POS, OFFLINE_SYNC, MIGRATION }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "txn_seq")
    @SequenceGenerator(name = "txn_seq", schema = "pos", sequenceName = "sales_transactions_transaction_id_seq", allocationSize = 50)
    @Column(name = "transaction_id")
    private Long id;

    @Column(name = "receipt_no", nullable = false, unique = true)
    private String receiptNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "terminal_id")
    private Long terminalId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Column(name = "cashier_id")
    private Long cashierId;

    /** Thành viên (module customer); NULL = khách vãng lai. */
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "captured_phone")
    private String capturedPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false)
    private Type txnType;

    @Column(name = "original_txn_id")
    private Long originalTxnId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "txn_time", nullable = false)
    private OffsetDateTime txnTime;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    @Column(name = "net_amount", nullable = false)
    private long netAmount;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Column(name = "eligible_amount")
    private Long eligibleAmount;

    @Column(name = "points_earned")
    private Integer pointsEarned;

    @Column(name = "points_redeemed")
    private Integer pointsRedeemed;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<TransactionLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<Payment> payments = new ArrayList<>();

    /** Tạo hóa đơn bán tại quầy; tổng tiền tính từ các dòng được thêm vào. */
    public static SalesTransaction sale(String receiptNo, Long storeId, Long terminalId, Long shiftId, Long cashierId,
                                        Long customerId, OffsetDateTime at) {
        SalesTransaction t = new SalesTransaction();
        t.receiptNo = receiptNo;
        t.storeId = storeId;
        t.terminalId = terminalId;
        t.shiftId = shiftId;
        t.cashierId = cashierId;
        t.customerId = customerId;
        t.txnType = Type.SALE;
        t.status = Status.COMPLETED;
        t.source = Source.POS;
        t.txnTime = at;
        t.syncedAt = at;
        return t;
    }

    public TransactionLine addLine(Long productId, BigDecimal quantity, long unitPrice, long lineAmount, boolean pointEligible) {
        TransactionLine l = new TransactionLine(this, productId, quantity, unitPrice, lineAmount, pointEligible);
        lines.add(l);
        netAmount += lineAmount;
        return l;
    }

    public Payment addPayment(Payment.Method method, long amount, Long redemptionId, String reference) {
        Payment p = new Payment(this, method, amount, redemptionId, reference);
        payments.add(p);
        return p;
    }

    /** Ghi kết quả loyalty lên hóa đơn: giá trị tính điểm, điểm cộng, điểm đã dùng. */
    public void applyLoyalty(long eligibleAmount, int pointsEarned, int pointsRedeemed) {
        this.eligibleAmount = eligibleAmount;
        this.pointsEarned = pointsEarned;
        this.pointsRedeemed = pointsRedeemed;
    }

    /** Tổng giá trị các dòng được tích điểm / được trừ bằng điểm (loại hàng hạn chế khuyến mại – BRL-05). */
    public long redeemableAmount() {
        return lines.stream().filter(TransactionLine::isPointEligible).mapToLong(TransactionLine::getLineAmount).sum();
    }

    public long cashPaid() {
        return payments.stream().filter(p -> p.getMethod() == Payment.Method.CASH).mapToLong(Payment::getAmount).sum();
    }
}
