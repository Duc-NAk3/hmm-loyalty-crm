package vn.sofitech.hmm.loyalty.api;

import lombok.Value;

@Value
public class RedemptionRequested {

    Long redemptionId;
    int points;
    long amountVnd;
    String status;
    String phoneMasked;
    int otpTtlMinutes;
}
