package vn.sofitech.hmm.shared;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Nhật ký thao tác nhạy cảm – chỉ ghi thêm (FR-67, dùng cho các thao tác có trong demo). */
@Service
public class AuditService {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public AuditService(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void log(Long userId, String action, String entityType, Object entityId, Object before, Object after) {
        jdbc.sql("""
                INSERT INTO shared.audit_logs (user_id, action, entity_type, entity_id, before_data, after_data, created_at)
                VALUES (:uid, :action, :etype, :eid, CAST(:before AS jsonb), CAST(:after AS jsonb), now())
                """)
                .param("uid", userId).param("action", action).param("etype", entityType)
                .param("eid", entityId == null ? null : String.valueOf(entityId))
                .param("before", before == null ? null : json.writeValueAsString(before))
                .param("after", after == null ? null : json.writeValueAsString(after))
                .update();
    }
}
