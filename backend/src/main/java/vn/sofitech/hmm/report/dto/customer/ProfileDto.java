package vn.sofitech.hmm.report.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    private Long customerId;
    private String memberCode;
    private String fullName;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String email;
    private String status;
    private OffsetDateTime verifiedAt;
    private String tierCode;
    private String tierName;
    private BigDecimal multiplier;
    private int pointBalance;
    private long spend12m;
    private boolean redeemLocked;
    private boolean employee;
    private String registeredStore;
    private String registeredStoreName;
    private OffsetDateTime createdAt;
    private Long mergedIntoId;
    private String nextTier;
    private Long nextThreshold;
    private long thresholdSpend;
}
