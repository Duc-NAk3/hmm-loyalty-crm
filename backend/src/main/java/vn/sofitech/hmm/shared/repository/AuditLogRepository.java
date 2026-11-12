package vn.sofitech.hmm.shared.repository;

import vn.sofitech.hmm.shared.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/** Nhật ký chỉ ghi thêm: code chỉ dùng save (insert). */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
