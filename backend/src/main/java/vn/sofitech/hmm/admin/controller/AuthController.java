package vn.sofitech.hmm.admin.controller;

import vn.sofitech.hmm.admin.dto.auth.LoginRequestDto;
import vn.sofitech.hmm.admin.dto.auth.LoginResponseDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.admin.service.AuthService;

/** Đăng nhập POS (kèm mã quầy) và Web Portal. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginRequestDto req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public CurrentUser me(@AuthenticationPrincipal Jwt jwt) {
        return CurrentUser.of(jwt);
    }
}
