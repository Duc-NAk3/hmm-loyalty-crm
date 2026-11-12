package vn.sofitech.hmm.loyalty.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.AdminFacade;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.customer.CustomerFacade.CustomerInfo;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionHeld;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionInfo;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionRequested;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.loyalty.domain.PointRedemption;
import vn.sofitech.hmm.loyalty.domain.PointRedemptionRepository;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.SmsRequestedEvent;
import vn.sofitech.hmm.shared.Texts;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Đổi điểm trừ tiền hóa đơn (FR-30): báo giá → OTP → giữ điểm tối đa 15 phút → xác nhận khi hóa đơn hoàn tất,
 * trả lại khi hủy hoặc quá hạn. Chuyển trạng thái do entity {@link PointRedemption} tự kiểm tra (State pattern).
 */
@Service
public class RedemptionService {

    private static final Logger log = LoggerFactory.getLogger(RedemptionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PointRedemptionRepository redemptions;
    private final CustomerFacade customers;
    private final LoyaltyConfigService config;
    private final LedgerService ledger;
    private final OtpStore otpStore;
    private final AdminFacade admin;
    private final ApplicationEventPublisher events;

    public RedemptionService(PointRedemptionRepository redemptions, CustomerFacade customers, LoyaltyConfigService config,
                             LedgerService ledger, OtpStore otpStore, AdminFacade admin, ApplicationEventPublisher events) {
        this.redemptions = redemptions;
        this.customers = customers;
        this.config = config;
        this.ledger = ledger;
        this.otpStore = otpStore;
        this.admin = admin;
        this.events = events;
    }

    static RedemptionInfo toInfo(PointRedemption r) {
        return new RedemptionInfo(r.getId(), r.getCustomerId(), r.getTerminalId(), r.getPoints(), r.getAmountVnd(), r.getStatus().name(),
                r.getHoldExpiresAt());
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(Texts.VN);
    }

    @Transactional(readOnly = true)
    public PointCalculator.RedeemQuote quote(long customerId, long redeemableAmount) {
        CustomerInfo c = customers.findById(customerId).orElseThrow(() -> ApiException.notFound("Không tìm thấy thành viên"));
        if (c.redeemLocked()) return new PointCalculator.RedeemQuote(false, 0, 0, "Tài khoản đang tạm khóa đổi điểm, mời khách liên hệ CSKH");
        if (!c.isActive()) return new PointCalculator.RedeemQuote(false, 0, 0, "Hồ sơ không ở trạng thái hoạt động");
        return PointCalculator.quote(redeemableAmount, c.pointBalance(), config.currentPolicy());
    }

    @Transactional
    public RedemptionRequested request(long customerId, long terminalId, int points, long redeemableAmount) {
        PointCalculator.RedeemQuote q = quote(customerId, redeemableAmount);
        if (!q.allowed()) throw ApiException.badRequest("REDEEM_NOT_ALLOWED", q.reason());
        PointCalculator.Policy policy = config.currentPolicy();
        if (points < policy.minRedeemPoints() || points > q.maxPoints())
            throw ApiException.badRequest("REDEEM_POINTS", "Số điểm phải từ " + policy.minRedeemPoints() + " đến " + q.maxPoints());
        // Hủy yêu cầu cũ còn treo của khách tại quầy này
        redemptions.findByCustomerIdAndTerminalIdAndStatus(customerId, terminalId, PointRedemption.Status.QUOTED)
                .forEach(PointRedemption::release);
        long amount = (long) points * policy.pointValueVnd();
        PointRedemption r = redemptions.save(new PointRedemption(customerId, terminalId, points, amount, now()));

        int len = admin.intParam("OTP_LENGTH", 6);
        int ttl = admin.intParam("OTP_TTL_MINUTES", 5);
        String otp = String.format("%0" + len + "d", RANDOM.nextInt((int) Math.pow(10, len)));
        otpStore.save(String.valueOf(r.getId()), otp, Duration.ofMinutes(ttl), admin.intParam("OTP_MAX_ATTEMPTS", 3));
        CustomerInfo c = customers.findById(customerId).orElseThrow();
        events.publishEvent(new SmsRequestedEvent("SMS_OTP", "point_redemptions", r.getId(), c.phone(),
                "Ma OTP doi " + points + " diem tai Hoa Mai Mart: " + otp + ". Hieu luc " + ttl + " phut. Khong chia se ma nay cho nguoi khac."));
        return new RedemptionRequested(r.getId(), points, amount, r.getStatus().name(), Texts.maskPhone(c.phone()), ttl);
    }

    /** Không rollback khi báo lỗi để giữ trạng thái hủy sau khi sai OTP quá số lần / OTP hết hạn. */
    @Transactional(noRollbackFor = ApiException.class)
    public RedemptionHeld verifyOtp(long redemptionId, long terminalId, String otp, long cashierId) {
        PointRedemption r = lockForTerminal(redemptionId, terminalId);
        if (r.getStatus() != PointRedemption.Status.QUOTED) throw ApiException.conflict("REDEEM_STATUS", "Yêu cầu đổi điểm không còn chờ OTP");
        OtpStore.Result res = otpStore.verify(String.valueOf(redemptionId), otp == null ? "" : otp.trim());
        switch (res) {
            case WRONG -> throw ApiException.badRequest("OTP_WRONG", "Mã OTP không đúng");
            case EXPIRED, TOO_MANY_ATTEMPTS -> {
                r.release();
                throw ApiException.badRequest("OTP_" + res.name(), res == OtpStore.Result.EXPIRED
                        ? "Mã OTP đã hết hạn, vui lòng gửi lại" : "Sai OTP quá số lần cho phép, yêu cầu đổi điểm đã bị hủy");
            }
            case OK -> {
            }
        }
        PointCalculator.Policy policy = config.currentPolicy();
        int balance = ledger.hold(r.getCustomerId(), r, r.getPoints(), policy, cashierId);
        OffsetDateTime now = now();
        r.hold(now, now.plusMinutes(policy.holdMinutes()));
        boolean firstVerification = customers.markVerified(r.getCustomerId());
        return new RedemptionHeld(r.getId(), r.getStatus().name(), r.getPoints(), r.getAmountVnd(), r.getHoldExpiresAt(), balance, firstVerification);
    }

    @Transactional
    public RedemptionInfo cancel(long redemptionId, long terminalId, long cashierId) {
        PointRedemption r = lockForTerminal(redemptionId, terminalId);
        if (r.isHeld()) ledger.release(r.getCustomerId(), r, r.getPoints(), config.currentPolicy(), cashierId);
        else otpStore.remove(String.valueOf(redemptionId));
        r.release();
        return toInfo(r);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public RedemptionInfo confirm(long redemptionId, long customerId, long terminalId, long transactionId, long cashierId) {
        PointRedemption r = redemptions.findForUpdate(redemptionId).orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu đổi điểm"));
        if (r.getCustomerId() != customerId || r.getTerminalId() != terminalId)
            throw ApiException.badRequest("REDEEM_MISMATCH", "Yêu cầu đổi điểm không khớp hóa đơn");
        r.confirm(transactionId, now()); // báo lỗi nếu đã hết hạn giữ điểm hoặc không ở trạng thái HELD
        ledger.redeem(customerId, r, transactionId, r.getPoints(), config.currentPolicy(), cashierId);
        return toInfo(r);
    }

    /** Tác vụ nền: điểm giữ quá 15 phút được trả lại (EXPIRED); yêu cầu chờ OTP quá 30 phút cũng hết hạn. */
    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    @Transactional
    public void expireHolds() {
        OffsetDateTime now = now();
        List<PointRedemption> held = redemptions.findByStatusAndHoldExpiresAtBefore(PointRedemption.Status.HELD, now);
        List<PointRedemption> quoted = redemptions.findByStatusAndCreatedAtBefore(PointRedemption.Status.QUOTED, now.minusMinutes(30));
        if (held.isEmpty() && quoted.isEmpty()) return;
        PointCalculator.Policy policy = config.currentPolicy();
        for (PointRedemption r : held) {
            ledger.release(r.getCustomerId(), r, r.getPoints(), policy, null);
            r.expire();
        }
        quoted.forEach(PointRedemption::expire);
        log.info("Expired {} held and {} quoted redemption(s)", held.size(), quoted.size());
    }

    private PointRedemption lockForTerminal(long redemptionId, long terminalId) {
        PointRedemption r = redemptions.findForUpdate(redemptionId).orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu đổi điểm"));
        if (r.getTerminalId() != terminalId) throw ApiException.forbidden("Yêu cầu đổi điểm không thuộc quầy này");
        return r;
    }
}
