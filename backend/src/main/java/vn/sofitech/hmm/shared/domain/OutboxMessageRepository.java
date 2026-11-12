package vn.sofitech.hmm.shared.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    /** SMS Gateway giả lập: đánh dấu đã gửi mọi tin đang chờ đến hạn (cập nhật hàng loạt bằng JPQL). */
    @Modifying
    @Query("""
            update OutboxMessage m set m.status = vn.sofitech.hmm.shared.domain.OutboxMessage.Status.SENT,
                   m.attempts = m.attempts + 1, m.sentAt = :now
            where m.status = vn.sofitech.hmm.shared.domain.OutboxMessage.Status.PENDING and m.nextAttemptAt <= :now
            """)
    int markPendingSent(@Param("now") OffsetDateTime now);

    /** Tin SMS mới nhất, lọc theo SĐT trong payload JSON (hàm jsonb_extract_path_text của PostgreSQL). */
    @Query("""
            select m from OutboxMessage m
            where m.messageType like 'SMS%'
              and (:phone is null or function('jsonb_extract_path_text', m.payload, 'phone') = :phone)
            order by m.id desc
            """)
    List<OutboxMessage> findLatestSms(@Param("phone") String phone, Pageable page);
}
