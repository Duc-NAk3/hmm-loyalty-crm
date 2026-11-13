package vn.sofitech.hmm.loyalty.api;

import lombok.Value;

import java.time.OffsetDateTime;

@Value
public class RedemptionHeld {

    Long redemptionId;
    String status;
    int points;
    long amountVnd;
    OffsetDateTime holdExpiresAt;
    int balanceAfterHold;
    boolean phoneVerified;
}
