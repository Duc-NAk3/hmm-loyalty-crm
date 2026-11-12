package vn.sofitech.hmm.admin.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionUser {

    private Long id;
    private String username;
    private String fullName;
    private String role;
    private Long storeId;
    private String storeCode;
    private String storeName;
}
