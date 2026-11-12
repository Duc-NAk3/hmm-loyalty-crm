package vn.sofitech.hmm.loyalty;

import java.time.OffsetDateTime;

/** API công khai của module loyalty (Facade): tích điểm cho hóa đơn, đổi điểm có OTP, thông tin hạng. */
public interface LoyaltyFacade {

    record RedemptionRequested(Long redemptionId, int points, long amountVnd, String status, String phoneMasked, int otpTtlMinutes) {
    }

    record RedemptionHeld(Long redemptionId, String status, int points, long amountVnd, OffsetDateTime holdExpiresAt,
                          int balanceAfterHold, boolean phoneVerified) {
    }

    record RedemptionInfo(Long redemptionId, Long customerId, Long terminalId, int points, long amountVnd, String status,
                          OffsetDateTime holdExpiresAt) {
    }

    /** Kết quả tích điểm cho một hóa đơn: điểm cộng, số dư sau cùng, hạng hiện tại, hạng mới nếu vừa lên hạng. */
    record EarnResult(int pointsEarned, int balance, String tierCode, String tierName, String upgradedToName) {
    }

    PointCalculator.Policy currentPolicy();

    String tierName(String tierCode);

    /** Kiểm tra điều kiện BRL-09 và tính số điểm tối đa trên giá trị hàng được đổi điểm. */
    PointCalculator.RedeemQuote quote(long customerId, long redeemableAmount);

    /** Tạo yêu cầu đổi điểm (QUOTED) và gửi OTP tới SĐT thành viên. */
    RedemptionRequested requestRedemption(long customerId, long terminalId, int points, long redeemableAmount);

    /** OTP đúng → giữ điểm (HELD); lần đầu đồng thời xác thực SĐT (FR-20). */
    RedemptionHeld verifyOtp(long redemptionId, long terminalId, String otp, long cashierId);

    RedemptionInfo cancelRedemption(long redemptionId, long terminalId, long cashierId);

    /** Gọi trong giao dịch hoàn tất hóa đơn: HELD → CONFIRMED, trừ điểm FIFO. */
    RedemptionInfo confirmRedemption(long redemptionId, long customerId, long terminalId, long transactionId, long cashierId);

    /** Tích điểm theo hạng tại thời điểm giao dịch (BRL-03, 04, 06), cộng chi tiêu và xét lên hạng (BRL-11). */
    EarnResult earnForSale(long customerId, long transactionId, long eligibleAmount, long saleTotal, OffsetDateTime at, long cashierId);
}
