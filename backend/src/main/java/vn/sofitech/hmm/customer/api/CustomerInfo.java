package vn.sofitech.hmm.customer.api;

import lombok.Value;

import java.time.OffsetDateTime;

@Value
public class CustomerInfo {

    Long customerId;
    String memberCode;
    String phone;
    String fullName;
    String status;
    String tierCode;
    int pointBalance;
    long spend12m;
    boolean redeemLocked;
    boolean employee;
    Long registeredStoreId;
    OffsetDateTime createdAt;

    public boolean isActive() {
        return "UNVERIFIED".equals(status) || "VERIFIED".equals(status);
    }
}
