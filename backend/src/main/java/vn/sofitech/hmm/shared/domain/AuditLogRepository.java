package vn.sofitech.hmm.shared.domain;

import org.springframework.data.jpa.repository.JpaRepository;

/** Nhật ký chỉ ghi thêm: code chỉ dùng save (insert). */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
