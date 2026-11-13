package vn.sofitech.hmm.admin.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long userId;
    private String username;
    private String fullName;
    private String phone;
    private String role;
    private String roleName;
    private Long storeId;
    private String storeCode;
    private String status;
    private OffsetDateTime lastLoginAt;
}
