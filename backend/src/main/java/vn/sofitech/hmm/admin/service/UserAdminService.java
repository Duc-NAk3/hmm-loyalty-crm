package vn.sofitech.hmm.admin.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.AdminFacade.TerminalInfo;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.admin.domain.*;
import vn.sofitech.hmm.admin.web.AdminDtos.*;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;

import java.util.List;
import java.util.Map;

/** Cửa hàng, quầy và người dùng – vai trò (FR-65). */
@Service
public class UserAdminService {

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final StoreRepository stores;
    private final PosTerminalRepository terminals;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserAdminService(AppUserRepository users, RoleRepository roles, StoreRepository stores, PosTerminalRepository terminals,
                            PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.roles = roles;
        this.stores = stores;
        this.terminals = terminals;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TerminalInfo> activeTerminals() {
        return terminals.findAllByStatusOrderByTerminalCode("ACTIVE").stream().map(AdminFacadeImpl::toInfo).toList();
    }

    @Transactional(readOnly = true)
    public List<StoreView> stores() {
        return stores.findAllByOrderByStoreCode().stream().map(s -> new StoreView(s.getId(), s.getStoreCode(), s.getName(),
                s.getRegion(), s.getAddress(), s.getStatus(), s.getOpenedAt(), terminals.countByStoreId(s.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleView> roles() {
        return roles.findAllByOrderById().stream().map(r -> new RoleView(r.getId(), r.getCode(), r.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<UserView> users() {
        return users.findAllByOrderByRoleIdAscUsernameAsc().stream().map(UserAdminService::toView).toList();
    }

    private static UserView toView(AppUser u) {
        Store s = u.getStore();
        return new UserView(u.getId(), u.getUsername(), u.getFullName(), u.getPhone(), u.getRole().getCode(), u.getRole().getName(),
                s == null ? null : s.getId(), s == null ? null : s.getStoreCode(), u.getStatus().name(), u.getLastLoginAt());
    }

    @Transactional
    public long create(UserRequest req, CurrentUser me) {
        if (req.password() == null) throw ApiException.badRequest("PASSWORD", "Cần nhập mật khẩu ban đầu (tối thiểu 6 ký tự)");
        Role role = role(req.role());
        Store store = store(req.role(), req.storeId());
        AppUser u = users.save(new AppUser(req.username().trim(), encoder.encode(req.password()), req.fullName().trim(), req.phone(), role, store));
        audit.log(me.id(), "CREATE_USER", "app_users", u.getId(), null, Map.of("username", u.getUsername(), "role", role.getCode()));
        return u.getId();
    }

    @Transactional
    public long update(long id, UserRequest req, CurrentUser me) {
        AppUser u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
        Role role = role(req.role());
        u.update(req.fullName().trim(), req.phone(), role, store(req.role(), req.storeId()));
        if (req.password() != null && !req.password().isBlank()) u.changePassword(encoder.encode(req.password()));
        audit.log(me.id(), "UPDATE_USER", "app_users", id, null, Map.of("role", role.getCode(), "storeId", String.valueOf(req.storeId())));
        return id;
    }

    @Transactional
    public void setStatus(long id, String status, CurrentUser me) {
        if (!List.of("ACTIVE", "LOCKED").contains(status)) throw ApiException.badRequest("STATUS", "Trạng thái không hợp lệ");
        if (me.id() == id) throw ApiException.badRequest("SELF", "Không thể tự khóa tài khoản của mình");
        AppUser u = users.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy người dùng"));
        u.setStatus(AppUser.Status.valueOf(status));
        audit.log(me.id(), "LOCKED".equals(status) ? "LOCK_USER" : "UNLOCK_USER", "app_users", id, null, Map.of("status", status));
    }

    private Role role(String code) {
        return roles.findByCode(code).orElseThrow(() -> ApiException.badRequest("ROLE", "Vai trò không hợp lệ"));
    }

    /** Thu ngân và quản lý cửa hàng phải gắn với một cửa hàng; vai trò trung tâm có thể để trống (toàn chuỗi). */
    private Store store(String role, Long storeId) {
        if (storeId == null) {
            if (List.of("CASHIER", "STORE_MANAGER").contains(role))
                throw ApiException.badRequest("STORE", "Thu ngân và quản lý cửa hàng phải gắn với một cửa hàng");
            return null;
        }
        return stores.findById(storeId).orElseThrow(() -> ApiException.badRequest("STORE", "Cửa hàng không tồn tại"));
    }
}
