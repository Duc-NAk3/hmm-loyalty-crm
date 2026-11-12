package vn.sofitech.hmm.admin.service;

import vn.sofitech.hmm.admin.dto.auth.LoginRequest;
import vn.sofitech.hmm.admin.dto.auth.SessionUser;
import vn.sofitech.hmm.admin.dto.auth.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.AdminFacade.TerminalInfo;
import vn.sofitech.hmm.admin.domain.AppUser;
import vn.sofitech.hmm.admin.repository.AppUserRepository;
import vn.sofitech.hmm.admin.domain.PosTerminal;
import vn.sofitech.hmm.admin.repository.PosTerminalRepository;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Đăng nhập POS và Web Portal, phát JWT (FR-01, FR-65). */
@Service
public class AuthService {

    private final AppUserRepository users;
    private final PosTerminalRepository terminals;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final long hours;

    public AuthService(AppUserRepository users, PosTerminalRepository terminals, PasswordEncoder encoder, JwtEncoder jwtEncoder,
                       @Value("${app.jwt-hours}") long hours) {
        this.users = users;
        this.terminals = terminals;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.hours = hours;
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        AppUser u = users.findByUsernameIgnoreCase(req.getUsername().trim())
                .filter(x -> encoder.matches(req.getPassword(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_LOGIN", "Sai tên đăng nhập hoặc mật khẩu"));
        if (!u.isActive()) throw new ApiException(HttpStatus.UNAUTHORIZED, "LOCKED", "Tài khoản đã bị khóa");
        String role = u.getRole().getCode();
        Long storeId = u.getStore() == null ? null : u.getStore().getId();

        TerminalInfo terminal = null;
        if (req.getTerminalCode() != null && !req.getTerminalCode().isBlank()) {
            // Đăng nhập POS: thu ngân phải thuộc cửa hàng của quầy
            if (!List.of("CASHIER", "STORE_MANAGER").contains(role)) throw ApiException.forbidden("Tài khoản này không được dùng POS");
            PosTerminal t = terminals.findByTerminalCodeAndStatus(req.getTerminalCode(), "ACTIVE")
                    .orElseThrow(() -> ApiException.badRequest("BAD_TERMINAL", "Quầy chưa được đăng ký"));
            if (storeId == null || !storeId.equals(t.getStore().getId()))
                throw ApiException.forbidden("Thu ngân không thuộc cửa hàng của quầy này");
            terminal = AdminFacadeImpl.toInfo(t);
        } else if ("CASHIER".equals(role)) {
            throw ApiException.forbidden("Thu ngân chỉ đăng nhập trên POS");
        }
        u.recordLogin(OffsetDateTime.now(Texts.VN));

        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().issuer("hmm-loyalty").subject(u.getUsername())
                .issuedAt(now).expiresAt(now.plus(hours, ChronoUnit.HOURS))
                .claim("uid", u.getId()).claim("name", u.getFullName()).claim("role", role).claim("roles", List.of(role));
        if (storeId != null) claims.claim("storeId", storeId);
        String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();

        SessionUser user = new SessionUser(u.getId(), u.getUsername(), u.getFullName(), role, storeId,
                u.getStore() == null ? null : u.getStore().getStoreCode(), u.getStore() == null ? null : u.getStore().getName());
        return new LoginResponse(token, user, terminal);
    }
}
