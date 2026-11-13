package vn.sofitech.hmm.loyalty;

import vn.sofitech.hmm.loyalty.api.EarnResult;
import vn.sofitech.hmm.loyalty.api.RedemptionHeld;
import vn.sofitech.hmm.loyalty.api.RedemptionInfo;
import vn.sofitech.hmm.loyalty.api.RedemptionRequested;

import java.time.OffsetDateTime;

/** API công khai của module loyalty (Facade): tích điểm cho hóa đơn, đổi điểm có OTP, thông tin hạng. */
public interface LoyaltyFacade {

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
