package vn.sofitech.hmm.customer.api;

import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Value
public class NewCustomer {

    String phone;
    String fullName;
    String gender;
    LocalDate dateOfBirth;
    String email;
    Long storeId;
    String status;
    OffsetDateTime createdAt;
}
