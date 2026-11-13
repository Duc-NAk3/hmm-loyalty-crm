package vn.sofitech.hmm.admin.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.sofitech.hmm.admin.api.TerminalInfo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String token;
    private SessionUserDto user;
    private TerminalInfo terminal;
}
