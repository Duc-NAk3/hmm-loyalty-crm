package vn.sofitech.hmm.loyalty.dto.member;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Đăng ký nhanh tại quầy (FR-19). {@code marketingConsent} không gửi thì coi như không đồng ý. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterMemberRequestDto {

    @NotBlank(message = "SĐT là bắt buộc")
    private String phone;
    @NotBlank(message = "Họ tên là bắt buộc")
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private Boolean marketingConsent;

    public boolean consentsToMarketing() {
        return Boolean.TRUE.equals(marketingConsent);
    }

}
