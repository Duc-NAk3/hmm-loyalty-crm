package vn.sofitech.hmm.loyalty.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.sofitech.hmm.shared.ApiException;

import java.time.OffsetDateTime;

/**
 * Một lần đổi điểm (loyalty.point_redemptions) – máy trạng thái:
 * QUOTED (đã gửi OTP) → HELD (OTP đúng, giữ điểm) → CONFIRMED (hóa đơn hoàn tất);
 * QUOTED/HELD → RELEASED (hủy, sai OTP) hoặc EXPIRED (quá hạn).
 */
@Entity
@Table(schema = "loyalty", name = "point_redemptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointRedemption {

    public enum Status { QUOTED, HELD, CONFIRMED, RELEASED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "redemption_seq")
    @SequenceGenerator(name = "redemption_seq", schema = "loyalty", sequenceName = "point_redemptions_redemption_id_seq", allocationSize = 50)
    @Column(name = "redemption_id")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "terminal_id", nullable = false)
    private Long terminalId;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(nullable = false)
    private int points;

    @Column(name = "amount_vnd", nullable = false)
    private long amountVnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "otp_verified_at")
    private OffsetDateTime otpVerifiedAt;

    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public PointRedemption(Long customerId, Long terminalId, int points, long amountVnd, OffsetDateTime createdAt) {
        this.customerId = customerId;
        this.terminalId = terminalId;
        this.points = points;
        this.amountVnd = amountVnd;
        this.createdAt = createdAt;
        this.status = Status.QUOTED;
    }

    /** OTP đúng: giữ điểm đến {@code expiresAt}. */
    public void hold(OffsetDateTime at, OffsetDateTime expiresAt) {
        require(Status.QUOTED, "Yêu cầu đổi điểm không còn chờ OTP");
        status = Status.HELD;
        otpVerifiedAt = at;
        holdExpiresAt = expiresAt;
    }

    /** Hóa đơn hoàn tất trong thời gian giữ điểm. */
    public void confirm(long transactionId, OffsetDateTime at) {
        if (status != Status.HELD || holdExpiresAt == null || holdExpiresAt.isBefore(at))
            throw ApiException.conflict("REDEEM_EXPIRED", "Điểm giữ chỗ đã hết hạn hoặc bị hủy, vui lòng đổi điểm lại");
        status = Status.CONFIRMED;
        this.transactionId = transactionId;
    }

    public void release() {
        if (status == Status.QUOTED || status == Status.HELD) status = Status.RELEASED;
    }

    public void expire() {
        if (status == Status.QUOTED || status == Status.HELD) status = Status.EXPIRED;
    }

    public boolean isHeld() {
        return status == Status.HELD;
    }

    /** Dùng cho dữ liệu mẫu: lần đổi điểm đã hoàn tất trong lịch sử. */
    public static PointRedemption confirmedHistory(Long customerId, Long terminalId, Long transactionId, int points, long amountVnd,
                                                   OffsetDateTime createdAt, OffsetDateTime otpAt) {
        PointRedemption r = new PointRedemption(customerId, terminalId, points, amountVnd, createdAt);
        r.status = Status.CONFIRMED;
        r.transactionId = transactionId;
        r.otpVerifiedAt = otpAt;
        r.holdExpiresAt = otpAt.plusMinutes(15);
        return r;
    }

    private void require(Status expected, String message) {
        if (status != expected) throw ApiException.conflict("REDEEM_STATUS", message);
    }
}
