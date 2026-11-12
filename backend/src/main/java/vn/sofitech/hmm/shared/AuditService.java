package vn.sofitech.hmm.shared;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.sofitech.hmm.shared.domain.AuditLog;
import vn.sofitech.hmm.shared.domain.AuditLogRepository;

import java.time.OffsetDateTime;

/** Nhật ký thao tác nhạy cảm – chỉ ghi thêm (FR-67, dùng cho các thao tác có trong demo). */
@Service
public class AuditService {

    private final AuditLogRepository repo;
    private final JsonMapper json;

    public AuditService(AuditLogRepository repo, JsonMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @Transactional
    public void log(Long userId, String action, String entityType, Object entityId, Object before, Object after) {
        repo.save(new AuditLog(userId, action, entityType, entityId == null ? null : String.valueOf(entityId),
                before == null ? null : json.writeValueAsString(before), after == null ? null : json.writeValueAsString(after),
                OffsetDateTime.now(Texts.VN)));
    }
}
