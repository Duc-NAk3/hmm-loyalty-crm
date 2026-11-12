package vn.sofitech.hmm.loyalty.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Lô điểm theo lần tích, để trừ và hết hạn theo FIFO (loyalty.point_lots). */
@Entity
@Table(schema = "loyalty", name = "point_lots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLot {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lot_seq")
    @SequenceGenerator(name = "lot_seq", schema = "loyalty", sequenceName = "point_lots_lot_id_seq", allocationSize = 50)
    @Column(name = "lot_id")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** Hóa đơn tạo ra lô điểm (module pos) – chỉ giữ ID. */
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "earned_at", nullable = false)
    private OffsetDateTime earnedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "points_earned", nullable = false)
    private int pointsEarned;

    @Column(name = "points_remaining", nullable = false)
    private int pointsRemaining;

    public PointLot(Long customerId, Long transactionId, OffsetDateTime earnedAt, OffsetDateTime expiresAt, int points) {
        this.customerId = customerId;
        this.transactionId = transactionId;
        this.earnedAt = earnedAt;
        this.expiresAt = expiresAt;
        this.pointsEarned = points;
        this.pointsRemaining = points;
    }

    /** Lấy tối đa {@code wanted} điểm từ lô; trả về số điểm thực lấy được. */
    public int consume(int wanted) {
        int take = Math.min(wanted, pointsRemaining);
        pointsRemaining -= take;
        return take;
    }

    /** Dùng cho dữ liệu mẫu: đặt số điểm còn lại sau khi mô phỏng FIFO. */
    public void setPointsRemaining(int remaining) {
        this.pointsRemaining = remaining;
    }
}
