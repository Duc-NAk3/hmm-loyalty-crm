package vn.sofitech.hmm.shared.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

/** Nhật ký thao tác nhạy cảm (shared.audit_logs) – chỉ ghi thêm. */
@Entity
@Immutable
@Table(schema = "shared", name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", schema = "shared", sequenceName = "audit_logs_audit_id_seq", allocationSize = 50)
    @Column(name = "audit_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "before_data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String afterData;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public AuditLog(Long userId, String action, String entityType, String entityId, String beforeData, String afterData,
                    OffsetDateTime createdAt) {
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.createdAt = createdAt;
    }
}
