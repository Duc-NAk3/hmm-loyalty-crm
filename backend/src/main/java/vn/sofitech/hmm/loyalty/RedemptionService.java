package vn.sofitech.hmm.loyalty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.ParameterService;
import vn.sofitech.hmm.customer.CustomerService;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.OutboxService;
import vn.sofitech.hmm.shared.Texts;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Đổi điểm trừ tiền hóa đơn (FR-30): báo giá → OTP → giữ điểm tối đa 15 phút → xác nhận khi hóa đơn hoàn tất,
 * trả lại khi hủy hoặc quá hạn. OTP đúng lần đầu đồng thời xác thực SĐT (FR-20).
 * Trạng thái: QUOTED → HELD → CONFIRMED | RELEASED | EXPIRED.
 */
@Service
public class RedemptionService {

    private static final Logger log = LoggerFactory.getLogger(RedemptionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Redemption(long redemptionId, long customerId, long terminalId, int points, long amountVnd, String status,
                             OffsetDateTime holdExpiresAt) {
    }

    private final JdbcClient jdbc;
    private final CustomerService customers;
    private final LoyaltyConfigService config;
    private final LedgerService ledger;
    private final OtpStore otpStore;
    private final OutboxService outbox;
    private final ParameterService params;

    public RedemptionService(JdbcClient jdbc, CustomerService customers, LoyaltyConfigService config, LedgerService ledger,
                             OtpStore otpStore, OutboxService outbox, ParameterService params) {
        this.jdbc = jdbc;
        this.customers = customers;
        this.config = config;
        this.ledger = ledger;
        this.otpStore = otpStore;
        this.outbox = outbox;
        this.params = params;
    }

    /** Kiểm tra điều kiện BRL-09 và tính số điểm tối đa trên giá trị các mặt hàng được phép đổi điểm. */
    public PointCalculator.RedeemQuote quote(long customerId, long redeemableLineAmount) {
        CustomerService.Customer c = customers.findById(customerId).orElseThrow(() -> ApiException.notFound("Không tìm thấy thành viên"));
        String blocked = blockedReason(c);
        if (blocked != null) return new PointCalculator.RedeemQuote(false, 0, 0, blocked);
        return PointCalculator.quote(redeemableLineAmount, c.pointBalance(), config.currentPolicy());
    }

    private String blockedReason(CustomerService.Customer c) {
        if (c.redeemLocked()) return "Tài khoản đang tạm khóa đổi điểm, mời khách liên hệ CSKH";
        if (!List.of("UNVERIFIED", "VERIFIED").contains(c.status())) return "Hồ sơ không ở trạng thái hoạt động";
        return null;
    }

    /** Tạo yêu cầu đổi điểm (QUOTED) và gửi OTP tới SĐT của thành viên. */
    @Transactional
    public Map<String, Object> request(long customerId, long terminalId, int points, long redeemableLineAmount) {
        PointCalculator.RedeemQuote q = quote(customerId, redeemableLineAmount);
        if (!q.allowed()) throw ApiException.badRequest("REDEEM_NOT_ALLOWED", q.reason());
        PointCalculator.Policy policy = config.currentPolicy();
        if (points < policy.minRedeemPoints() || points > q.maxPoints())
            throw ApiException.badRequest("REDEEM_POINTS", "Số điểm phải từ " + policy.minRedeemPoints() + " đến " + q.maxPoints());
        // Hủy yêu cầu cũ còn treo của khách tại quầy này
        jdbc.sql("UPDATE loyalty.point_redemptions SET status = 'RELEASED' WHERE customer_id = :c AND terminal_id = :t AND status = 'QUOTED'")
                .param("c", customerId).param("t", terminalId).update();
        long amount = (long) points * policy.pointValueVnd();
        long id = jdbc.sql("""
                INSERT INTO loyalty.point_redemptions (customer_id, terminal_id, points, amount_vnd, status, created_at)
                VALUES (:c, :t, :p, :a, 'QUOTED', now()) RETURNING redemption_id
                """).param("c", customerId).param("t", terminalId).param("p", points).param("a", amount).query(Long.class).single();
        int len = params.getInt("OTP_LENGTH", 6);
        int ttl = params.getInt("OTP_TTL_MINUTES", 5);
        String otp = String.format("%0" + len + "d", RANDOM.nextInt((int) Math.pow(10, len)));
        otpStore.save(String.valueOf(id), otp, Duration.ofMinutes(ttl), params.getInt("OTP_MAX_ATTEMPTS", 3));
        CustomerService.Customer c = customers.findById(customerId).orElseThrow();
        outbox.enqueueSms("SMS_OTP", "point_redemptions", id, c.phone(),
                "Ma OTP doi " + points + " diem tai Hoa Mai Mart: " + otp + ". Hieu luc " + ttl + " phut. Khong chia se ma nay cho nguoi khac.");
        return Map.of("redemptionId", id, "points", points, "amountVnd", amount, "status", "QUOTED",
                "phoneMasked", Texts.maskPhone(c.phone()), "otpTtlMinutes", ttl);
    }

    /** Xác thực OTP → giữ điểm (HELD). Không rollback khi báo lỗi để giữ trạng thái hủy sau khi sai OTP quá số lần. */
    @Transactional(noRollbackFor = ApiException.class)
    public Map<String, Object> verifyOtp(long redemptionId, long terminalId, String otp, long cashierId) {
        Redemption r = lockRedemption(redemptionId);
        if (r.terminalId() != terminalId) throw ApiException.forbidden("Yêu cầu đổi điểm không thuộc quầy này");
        if (!"QUOTED".equals(r.status())) throw ApiException.conflict("REDEEM_STATUS", "Yêu cầu đổi điểm không còn chờ OTP");
        OtpStore.Result res = otpStore.verify(String.valueOf(redemptionId), otp == null ? "" : otp.trim());
        switch (res) {
            case WRONG -> throw ApiException.badRequest("OTP_WRONG", "Mã OTP không đúng");
            case EXPIRED, TOO_MANY_ATTEMPTS -> {
                setStatus(redemptionId, "RELEASED");
                throw ApiException.badRequest("OTP_" + res.name(), res == OtpStore.Result.EXPIRED
                        ? "Mã OTP đã hết hạn, vui lòng gửi lại" : "Sai OTP quá số lần cho phép, yêu cầu đổi điểm đã bị hủy");
            }
            case OK -> {
            }
        }
        PointCalculator.Policy policy = config.currentPolicy();
        int balance = ledger.hold(r.customerId(), redemptionId, r.points(), policy, cashierId);
        OffsetDateTime expires = OffsetDateTime.now(Texts.VN).plusMinutes(policy.holdMinutes());
        jdbc.sql("UPDATE loyalty.point_redemptions SET status = 'HELD', otp_verified_at = now(), hold_expires_at = :e WHERE redemption_id = :id")
                .param("e", expires).param("id", redemptionId).update();
        boolean firstVerification = customers.markVerified(r.customerId());
        return Map.of("redemptionId", redemptionId, "status", "HELD", "points", r.points(), "amountVnd", r.amountVnd(),
                "holdExpiresAt", expires, "balanceAfterHold", balance, "phoneVerified", firstVerification);
    }

    @Transactional
    public Map<String, Object> cancel(long redemptionId, long terminalId, long cashierId) {
        Redemption r = lockRedemption(redemptionId);
        if (r.terminalId() != terminalId) throw ApiException.forbidden("Yêu cầu đổi điểm không thuộc quầy này");
        if ("HELD".equals(r.status())) {
            ledger.release(r.customerId(), redemptionId, r.points(), config.currentPolicy(), cashierId);
            setStatus(redemptionId, "RELEASED");
        } else if ("QUOTED".equals(r.status())) {
            otpStore.remove(String.valueOf(redemptionId));
            setStatus(redemptionId, "RELEASED");
        }
        return Map.of("redemptionId", redemptionId, "status", "RELEASED");
    }

    /** Gọi trong giao dịch hoàn tất hóa đơn: HELD → CONFIRMED, trừ điểm FIFO. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Redemption confirm(long redemptionId, long customerId, long terminalId, long transactionId, long cashierId) {
        Redemption r = lockRedemption(redemptionId);
        if (r.customerId() != customerId || r.terminalId() != terminalId)
            throw ApiException.badRequest("REDEEM_MISMATCH", "Yêu cầu đổi điểm không khớp hóa đơn");
        if (!"HELD".equals(r.status()) || r.holdExpiresAt() == null || r.holdExpiresAt().isBefore(OffsetDateTime.now()))
            throw ApiException.conflict("REDEEM_EXPIRED", "Điểm giữ chỗ đã hết hạn hoặc bị hủy, vui lòng đổi điểm lại");
        ledger.redeem(customerId, redemptionId, transactionId, r.points(), config.currentPolicy(), cashierId);
        jdbc.sql("UPDATE loyalty.point_redemptions SET status = 'CONFIRMED', transaction_id = :t WHERE redemption_id = :id")
                .param("t", transactionId).param("id", redemptionId).update();
        return r;
    }

    /** Tác vụ nền: điểm giữ quá 15 phút được trả lại (EXPIRED). */
    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    @Transactional
    public void expireHolds() {
        List<Redemption> expired = jdbc.sql("""
                SELECT redemption_id, customer_id, terminal_id, points, amount_vnd, status, hold_expires_at FROM loyalty.point_redemptions
                WHERE status = 'HELD' AND hold_expires_at < now() FOR UPDATE SKIP LOCKED
                """).query(this::map).list();
        if (expired.isEmpty() && jdbc.sql("SELECT 1 FROM loyalty.point_redemptions WHERE status = 'QUOTED' AND created_at < now() - interval '30 minutes' LIMIT 1")
                .query(Integer.class).optional().isEmpty()) return;
        PointCalculator.Policy policy = config.currentPolicy();
        for (Redemption r : expired) {
            ledger.release(r.customerId(), r.redemptionId(), r.points(), policy, null);
            setStatus(r.redemptionId(), "EXPIRED");
        }
        int q = jdbc.sql("UPDATE loyalty.point_redemptions SET status = 'EXPIRED' WHERE status = 'QUOTED' AND created_at < now() - interval '30 minutes'").update();
        if (!expired.isEmpty() || q > 0) log.info("Expired {} held and {} quoted redemption(s)", expired.size(), q);
    }

    private Redemption lockRedemption(long id) {
        return jdbc.sql("""
                SELECT redemption_id, customer_id, terminal_id, points, amount_vnd, status, hold_expires_at
                FROM loyalty.point_redemptions WHERE redemption_id = :id FOR UPDATE
                """).param("id", id).query(this::map).optional().orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu đổi điểm"));
    }

    private Redemption map(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        return new Redemption(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getInt(4), rs.getLong(5), rs.getString(6),
                rs.getObject(7, OffsetDateTime.class));
    }

    private void setStatus(long id, String status) {
        jdbc.sql("UPDATE loyalty.point_redemptions SET status = :s WHERE redemption_id = :id").param("s", status).param("id", id).update();
    }
}
