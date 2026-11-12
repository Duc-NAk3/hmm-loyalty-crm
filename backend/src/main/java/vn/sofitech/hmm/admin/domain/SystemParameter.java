package vn.sofitech.hmm.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Tham số hệ thống cấu hình được (admin.system_parameters), ví dụ OTP_TTL_MINUTES. */
@Entity
@Table(schema = "admin", name = "system_parameters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemParameter {

    @Id
    @Column(name = "param_key")
    private String key;

    @Column(name = "param_value", nullable = false)
    private String value;

    private String description;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public SystemParameter(String key, String value, String description, Long updatedBy, OffsetDateTime updatedAt) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }
}
