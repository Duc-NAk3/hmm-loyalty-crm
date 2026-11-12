package vn.sofitech.hmm.pos.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Dòng hàng của hóa đơn (pos.transaction_lines). Đơn giá là giá tại thời điểm bán. */
@Entity
@Table(schema = "pos", name = "transaction_lines")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "line_seq")
    @SequenceGenerator(name = "line_seq", schema = "pos", sequenceName = "transaction_lines_line_id_seq", allocationSize = 50)
    @Column(name = "line_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id")
    private SalesTransaction transaction;

    /** Mặt hàng (module catalog) – chỉ giữ ID. */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private long discount;

    @Column(name = "line_amount", nullable = false)
    private long lineAmount;

    @Column(name = "point_eligible", nullable = false)
    private boolean pointEligible;

    TransactionLine(SalesTransaction transaction, Long productId, BigDecimal quantity, long unitPrice, long lineAmount, boolean pointEligible) {
        this.transaction = transaction;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineAmount = lineAmount;
        this.pointEligible = pointEligible;
    }
}
