package vn.sofitech.hmm.loyalty.api;

import lombok.Value;

/** Kết quả tích điểm cho một hóa đơn: điểm cộng, số dư sau cùng, hạng hiện tại, hạng mới nếu vừa lên hạng. */
@Value
public class EarnResult {

    int pointsEarned;
    int balance;
    String tierCode;
    String tierName;
    String upgradedToName;
}
