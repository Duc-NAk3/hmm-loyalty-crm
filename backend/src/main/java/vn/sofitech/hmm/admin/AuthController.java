package vn.sofitech.hmm.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.shared.ApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Đăng nhập POS và Web Portal (FR-01, FR-65). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final long hours;

    public AuthController(JdbcClient jdbc, PasswordEncoder encoder, JwtEncoder jwtEncoder, @Value("${app.jwt-hours}") long hours) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.hours = hours;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password, String terminalCode) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        Map<String, Object> u = jdbc.sql("""
                SELECT u.user_id, u.username, u.password_hash, u.full_name, u.status, u.store_id, r.code AS role,
                       s.store_code, s.name AS store_name
                FROM admin.app_users u JOIN admin.roles r ON r.role_id = u.role_id
                LEFT JOIN admin.stores s ON s.store_id = u.store_id
                WHERE lower(u.username) = lower(:username)
                """).param("username", req.username().trim()).query().listOfRows().stream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_LOGIN", "Sai tên đăng nhập hoặc mật khẩu"));
        if (!encoder.matches(req.password(), (String) u.get("password_hash")))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "BAD_LOGIN", "Sai tên đăng nhập hoặc mật khẩu");
        if (!"ACTIVE".equals(u.get("status")))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "LOCKED", "Tài khoản đã bị khóa");
        String role = (String) u.get("role");
        Long storeId = u.get("store_id") == null ? null : ((Number) u.get("store_id")).longValue();

        Map<String, Object> terminal = null;
        if (req.terminalCode() != null && !req.terminalCode().isBlank()) {
            // Đăng nhập POS: thu ngân phải thuộc cửa hàng của quầy
            if (!List.of("CASHIER", "STORE_MANAGER").contains(role))
                throw ApiException.forbidden("Tài khoản này không được dùng POS");
            terminal = jdbc.sql("SELECT terminal_id, terminal_code, store_id FROM admin.pos_terminals WHERE terminal_code = :c AND status = 'ACTIVE'")
                    .param("c", req.terminalCode()).query().listOfRows().stream().findFirst()
                    .orElseThrow(() -> ApiException.badRequest("BAD_TERMINAL", "Quầy chưa được đăng ký"));
            if (storeId == null || storeId != ((Number) terminal.get("store_id")).longValue())
                throw ApiException.forbidden("Thu ngân không thuộc cửa hàng của quầy này");
        } else if ("CASHIER".equals(role)) {
            throw ApiException.forbidden("Thu ngân chỉ đăng nhập trên POS");
        }

        long uid = ((Number) u.get("user_id")).longValue();
        jdbc.sql("UPDATE admin.app_users SET last_login_at = now() WHERE user_id = :id").param("id", uid).update();
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer("hmm-loyalty").subject((String) u.get("username"))
                .issuedAt(now).expiresAt(now.plus(hours, ChronoUnit.HOURS))
                .claim("uid", uid).claim("name", u.get("full_name")).claim("role", role).claim("roles", List.of(role));
        if (storeId != null) claims.claim("storeId", storeId);
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", uid);
        user.put("username", u.get("username"));
        user.put("fullName", u.get("full_name"));
        user.put("role", role);
        user.put("storeId", storeId);
        user.put("storeCode", u.get("store_code"));
        user.put("storeName", u.get("store_name"));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("token", token);
        res.put("user", user);
        res.put("terminal", terminal);
        return res;
    }

    @GetMapping("/me")
    public CurrentUser me(@AuthenticationPrincipal Jwt jwt) {
        return CurrentUser.of(jwt);
    }
}
