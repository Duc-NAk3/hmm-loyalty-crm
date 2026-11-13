package vn.sofitech.hmm.loyalty.api;

import lombok.Value;

import java.time.OffsetDateTime;

@Value
public class RedemptionInfo {

    Long redemptionId;
    Long customerId;
    Long terminalId;
    int points;
    long amountVnd;
    String status;
    OffsetDateTime holdExpiresAt;
}
