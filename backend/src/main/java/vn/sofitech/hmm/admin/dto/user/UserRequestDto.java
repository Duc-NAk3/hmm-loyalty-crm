package vn.sofitech.hmm.admin.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {

    @NotBlank
    @Size(max = 50)
    private String username;
    @NotBlank
    private String fullName;
    private String phone;
    @NotBlank
    private String role;
    private Long storeId;
    @Size(min = 6)
    private String password;
}
