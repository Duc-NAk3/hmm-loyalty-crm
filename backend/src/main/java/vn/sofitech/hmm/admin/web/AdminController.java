package vn.sofitech.hmm.admin.web;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.AdminFacade.TerminalInfo;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.admin.service.UserAdminService;
import vn.sofitech.hmm.admin.web.AdminDtos.*;

import java.util.List;

/** Cửa hàng, quầy và người dùng – vai trò (FR-65). Quyền theo URL cấu hình trong SecurityConfig. */
@RestController
public class AdminController {

    private final UserAdminService service;

    public AdminController(UserAdminService service) {
        this.service = service;
    }

    /** Danh sách quầy để cài đặt máy POS lần đầu (giả lập đăng ký thiết bị). */
    @GetMapping("/api/pos/terminals")
    public List<TerminalInfo> terminals() {
        return service.activeTerminals();
    }

    @GetMapping("/api/portal/stores")
    public List<StoreView> stores() {
        return service.stores();
    }

    @GetMapping("/api/portal/users")
    public List<UserView> users() {
        return service.users();
    }

    @GetMapping("/api/portal/users/roles")
    public List<RoleView> roles() {
        return service.roles();
    }

    @PostMapping("/api/portal/users")
    public IdResponse create(@Valid @RequestBody UserRequest req, @AuthenticationPrincipal Jwt jwt) {
        return new IdResponse(service.create(req, CurrentUser.of(jwt)));
    }

    @PutMapping("/api/portal/users/{id}")
    public IdResponse update(@PathVariable long id, @Valid @RequestBody UserRequest req, @AuthenticationPrincipal Jwt jwt) {
        return new IdResponse(service.update(id, req, CurrentUser.of(jwt)));
    }

    @PostMapping("/api/portal/users/{id}/status")
    public IdResponse setStatus(@PathVariable long id, @Valid @RequestBody StatusRequest req, @AuthenticationPrincipal Jwt jwt) {
        service.setStatus(id, req.status(), CurrentUser.of(jwt));
        return new IdResponse(id);
    }
}
