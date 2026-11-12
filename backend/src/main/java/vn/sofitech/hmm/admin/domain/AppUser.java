package vn.sofitech.hmm.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Người dùng nội bộ (admin.app_users). Mật khẩu chỉ lưu dạng băm bcrypt. */
@Entity
@Table(schema = "admin", name = "app_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser {

    public enum Status { ACTIVE, LOCKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id")
    private Role role;

    /** NULL = phạm vi toàn chuỗi. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    public AppUser(String username, String passwordHash, String fullName, String phone, Role role, Store store) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.store = store;
        this.status = Status.ACTIVE;
    }

    public void update(String fullName, String phone, Role role, Store store) {
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.store = store;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void recordLogin(OffsetDateTime at) {
        this.lastLoginAt = at;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }
}
