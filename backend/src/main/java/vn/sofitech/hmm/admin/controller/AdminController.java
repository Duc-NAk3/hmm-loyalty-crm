package vn.sofitech.hmm.admin.controller;

import vn.sofitech.hmm.admin.dto.store.StoreDto;
import vn.sofitech.hmm.admin.dto.user.RoleDto;
import vn.sofitech.hmm.admin.dto.user.UserDto;
import vn.sofitech.hmm.admin.dto.user.UserRequestDto;
import vn.sofitech.hmm.admin.dto.user.StatusRequestDto;
import vn.sofitech.hmm.admin.dto.common.IdResponseDto;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.api.TerminalInfo;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.admin.service.UserAdminService;

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
    public List<StoreDto> stores() {
        return service.stores();
    }

    @GetMapping("/api/portal/users")
    public List<UserDto> users() {
        return service.users();
    }

    @GetMapping("/api/portal/users/roles")
    public List<RoleDto> roles() {
        return service.roles();
    }

    @PostMapping("/api/portal/users")
    public IdResponseDto create(@Valid @RequestBody UserRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        return new IdResponseDto(service.create(req, CurrentUser.of(jwt)));
    }

    @PutMapping("/api/portal/users/{id}")
    public IdResponseDto update(@PathVariable long id, @Valid @RequestBody UserRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        return new IdResponseDto(service.update(id, req, CurrentUser.of(jwt)));
    }

    @PostMapping("/api/portal/users/{id}/status")
    public IdResponseDto setStatus(@PathVariable long id, @Valid @RequestBody StatusRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        service.setStatus(id, req.getStatus(), CurrentUser.of(jwt));
        return new IdResponseDto(id);
    }
}
