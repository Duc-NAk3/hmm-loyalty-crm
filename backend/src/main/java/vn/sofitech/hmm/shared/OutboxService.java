package vn.sofitech.hmm.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Hộp thư đi" (outbox): việc cần gửi ra ngoài được ghi cùng giao dịch nghiệp vụ, tác vụ nền gửi sau.
 * Bản demo không có SMS Gateway thật: tác vụ nền đánh dấu SENT, nội dung xem tại trang "Hộp thư SMS giả lập".
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);
    private final JdbcClient jdbc;
    private final JsonMapper json;

    public OutboxService(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void enqueueSms(String messageType, String aggregateType, long aggregateId, String phone, String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phone", phone);
        payload.put("text", text);
        jdbc.sql("""
                INSERT INTO shared.outbox_messages (message_type, aggregate_type, aggregate_id, payload, status, attempts, next_attempt_at, created_at)
                VALUES (:type, :aggType, :aggId, CAST(:payload AS jsonb), 'PENDING', 0, now(), now())
                """)
                .param("type", messageType).param("aggType", aggregateType).param("aggId", aggregateId)
                .param("payload", json.writeValueAsString(payload))
                .update();
    }

    /** SMS Gateway giả lập: "gửi" các tin đang chờ mỗi 3 giây. */
    @Scheduled(fixedDelay = 3000)
    public void mockSend() {
        int n = jdbc.sql("""
                UPDATE shared.outbox_messages SET status = 'SENT', attempts = attempts + 1, sent_at = now()
                WHERE status = 'PENDING' AND next_attempt_at <= now()
                """).update();
        if (n > 0) log.info("Mock SMS gateway: sent {} message(s)", n);
    }

    public List<Map<String, Object>> latestSms(String phone, int limit) {
        return jdbc.sql("""
                SELECT outbox_id, message_type, payload->>'phone' AS phone, payload->>'text' AS text, status, created_at, sent_at
                FROM shared.outbox_messages
                WHERE message_type LIKE 'SMS_%' AND (CAST(:phone AS varchar) IS NULL OR payload->>'phone' = :phone)
                ORDER BY outbox_id DESC LIMIT :limit
                """).param("phone", phone).param("limit", limit).query().listOfRows();
    }
}
