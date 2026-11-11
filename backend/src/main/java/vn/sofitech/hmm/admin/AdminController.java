package vn.sofitech.hmm.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;

import java.util.List;
import java.util.Map;

/** Cửa hàng, quầy và người dùng – vai trò (FR-65; danh mục cửa hàng/quầy ở mức cần cho demo). */
@RestController
public class AdminController {

    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AdminController(JdbcClient jdbc, PasswordEncoder encoder, AuditService audit) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.audit = audit;
    }

    /** Danh sách quầy để cài đặt máy POS lần đầu (giả lập đăng ký thiết bị). */
    @GetMapping("/api/pos/terminals")
    public List<Map<String, Object>> terminals() {
        return jdbc.sql("""
                SELECT t.terminal_id, t.terminal_code, s.store_id, s.store_code, s.name AS store_name
                FROM admin.pos_terminals t JOIN admin.stores s ON s.store_id = t.store_id
                WHERE t.status = 'ACTIVE' ORDER BY t.terminal_code
                """).query().listOfRows();
    }

    @GetMapping("/api/portal/stores")
    public List<Map<String, Object>> stores() {
        return jdbc.sql("""
                SELECT s.store_id, s.store_code, s.name, s.region, s.address, s.status, s.opened_at,
                       (SELECT count(*) FROM admin.pos_terminals t WHERE t.store_id = s.store_id) AS terminal_count
                FROM admin.stores s ORDER BY s.store_code
                """).query().listOfRows();
    }

    @GetMapping("/api/portal/users")
    public List<Map<String, Object>> users() {
        return jdbc.sql("""
                SELECT u.user_id, u.username, u.full_name, u.phone, r.code AS role, r.name AS role_name,
                       u.store_id, s.store_code, u.status, u.last_login_at
                FROM admin.app_users u JOIN admin.roles r ON r.role_id = u.role_id
                LEFT JOIN admin.stores s ON s.store_id = u.store_id
                ORDER BY r.role_id, u.username
                """).query().listOfRows();
    }

    @GetMapping("/api/portal/users/roles")
    public List<Map<String, Object>> roles() {
        return jdbc.sql("SELECT role_id, code, name FROM admin.roles ORDER BY role_id").query().listOfRows();
    }

    public record UserRequest(@NotBlank @Size(max = 50) String username, @NotBlank String fullName, String phone,
                              @NotBlank String role, Long storeId, @Size(min = 6) String password) {
    }

    @PostMapping("/api/portal/users")
    @Transactional
    public Map<String, Object> createUser(@Valid @RequestBody UserRequest req, @AuthenticationPrincipal Jwt jwt) {
        if (req.password() == null) throw ApiException.badRequest("PASSWORD", "Cần nhập mật khẩu ban đầu (tối thiểu 6 ký tự)");
        long roleId = roleId(req.role());
        checkStoreRule(req.role(), req.storeId());
        Long id = jdbc.sql("""
                INSERT INTO admin.app_users (username, password_hash, full_name, phone, role_id, store_id, mfa_enabled, status)
                VALUES (:u, :p, :n, :phone, :r, :s, false, 'ACTIVE') RETURNING user_id
                """).param("u", req.username().trim()).param("p", encoder.encode(req.password())).param("n", req.fullName().trim())
                .param("phone", req.phone()).param("r", roleId).param("s", req.storeId()).query(Long.class).single();
        audit.log(CurrentUser.of(jwt).id(), "CREATE_USER", "app_users", id, null, Map.of("username", req.username(), "role", req.role()));
        return Map.of("userId", id);
    }

    @PutMapping("/api/portal/users/{id}")
    @Transactional
    public Map<String, Object> updateUser(@PathVariable long id, @Valid @RequestBody UserRequest req, @AuthenticationPrincipal Jwt jwt) {
        long roleId = roleId(req.role());
        checkStoreRule(req.role(), req.storeId());
        int n = jdbc.sql("UPDATE admin.app_users SET full_name = :n, phone = :phone, role_id = :r, store_id = :s WHERE user_id = :id")
                .param("n", req.fullName().trim()).param("phone", req.phone()).param("r", roleId).param("s", req.storeId()).param("id", id).update();
        if (n == 0) throw ApiException.notFound("Không tìm thấy người dùng");
        if (req.password() != null && !req.password().isBlank())
            jdbc.sql("UPDATE admin.app_users SET password_hash = :p WHERE user_id = :id").param("p", encoder.encode(req.password())).param("id", id).update();
        audit.log(CurrentUser.of(jwt).id(), "UPDATE_USER", "app_users", id, null, Map.of("role", req.role(), "storeId", String.valueOf(req.storeId())));
        return Map.of("userId", id);
    }

    public record StatusRequest(@NotNull String status) {
    }

    @PostMapping("/api/portal/users/{id}/status")
    @Transactional
    public Map<String, Object> setStatus(@PathVariable long id, @Valid @RequestBody StatusRequest req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser me = CurrentUser.of(jwt);
        if (!List.of("ACTIVE", "LOCKED").contains(req.status())) throw ApiException.badRequest("STATUS", "Trạng thái không hợp lệ");
        if (me.id() == id) throw ApiException.badRequest("SELF", "Không thể tự khóa tài khoản của mình");
        jdbc.sql("UPDATE admin.app_users SET status = :st WHERE user_id = :id").param("st", req.status()).param("id", id).update();
        audit.log(me.id(), "LOCKED".equals(req.status()) ? "LOCK_USER" : "UNLOCK_USER", "app_users", id, null, Map.of("status", req.status()));
        return Map.of("userId", id, "status", req.status());
    }

    private long roleId(String code) {
        return jdbc.sql("SELECT role_id FROM admin.roles WHERE code = :c").param("c", code).query(Long.class).optional()
                .orElseThrow(() -> ApiException.badRequest("ROLE", "Vai trò không hợp lệ"));
    }

    private void checkStoreRule(String role, Long storeId) {
        if (List.of("CASHIER", "STORE_MANAGER").contains(role) && storeId == null)
            throw ApiException.badRequest("STORE", "Thu ngân và quản lý cửa hàng phải gắn với một cửa hàng");
    }
}
