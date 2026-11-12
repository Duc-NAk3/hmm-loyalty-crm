package vn.sofitech.hmm.admin.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.sofitech.hmm.admin.AdminFacade.TerminalInfo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** DTO (request/response) của module admin – entity không bao giờ trả thẳng ra API. */
public final class AdminDtos {

    private AdminDtos() {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password, String terminalCode) {
    }

    public record SessionUser(Long id, String username, String fullName, String role, Long storeId, String storeCode, String storeName) {
    }

    public record LoginResponse(String token, SessionUser user, TerminalInfo terminal) {
    }

    public record StoreView(Long storeId, String storeCode, String name, String region, String address, String status,
                            LocalDate openedAt, long terminalCount) {
    }

    public record RoleView(Long roleId, String code, String name) {
    }

    public record UserView(Long userId, String username, String fullName, String phone, String role, String roleName,
                           Long storeId, String storeCode, String status, OffsetDateTime lastLoginAt) {
    }

    public record UserRequest(@NotBlank @Size(max = 50) String username, @NotBlank String fullName, String phone,
                              @NotBlank String role, Long storeId, @Size(min = 6) String password) {
    }

    public record StatusRequest(@NotNull String status) {
    }

    public record IdResponse(Long id) {
    }
}
