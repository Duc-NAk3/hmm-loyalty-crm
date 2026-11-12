package vn.sofitech.hmm.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Vai trò người dùng (admin.roles): ADMIN, STORE_MANAGER, CASHIER, CSKH, MARKETING, CATALOG. */
@Entity
@Table(schema = "admin", name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    public Role(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
