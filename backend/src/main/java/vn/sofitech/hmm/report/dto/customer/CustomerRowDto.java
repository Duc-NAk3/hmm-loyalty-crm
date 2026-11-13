package vn.sofitech.hmm.report.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRowDto {

    private Long customerId;
    private String memberCode;
    private String fullName;
    private String phone;
    private String tierCode;
    private String tierName;
    private String status;
    private int pointBalance;
    private long spend12m;
    private String registeredStore;
    private OffsetDateTime createdAt;
}
