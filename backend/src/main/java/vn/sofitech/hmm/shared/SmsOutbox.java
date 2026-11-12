package vn.sofitech.hmm.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.sofitech.hmm.shared.domain.OutboxMessage;
import vn.sofitech.hmm.shared.repository.OutboxMessageRepository;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Hộp thư đi" (outbox) cho SMS. Bản demo không có SMS Gateway thật: tác vụ nền đánh dấu SENT,
 * nội dung xem tại trang "Hộp thư SMS giả lập" hoặc "điện thoại khách giả lập" trên POS.
 */
@Service
public class SmsOutbox {

    /** Tin SMS đã ghi ra hộp thư đi. */
    public record SmsView(Long outboxId, String messageType, String phone, String text, String status,
                          OffsetDateTime createdAt, OffsetDateTime sentAt) {
    }

    private static final Logger log = LoggerFactory.getLogger(SmsOutbox.class);
    private final OutboxMessageRepository repo;
    private final JsonMapper json;

    public SmsOutbox(OutboxMessageRepository repo, JsonMapper json) {
        this.repo = repo;
        this.json = json;
    }

    /** Lắng nghe đồng bộ: ghi outbox trong cùng giao dịch với nghiệp vụ phát sự kiện. */
    @EventListener
    public void onSmsRequested(SmsRequestedEvent e) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phone", e.phone());
        payload.put("text", e.text());
        repo.save(new OutboxMessage(e.messageType(), e.aggregateType(), e.aggregateId(), json.writeValueAsString(payload),
                OffsetDateTime.now(Texts.VN)));
    }

    /** SMS Gateway giả lập: "gửi" các tin đang chờ mỗi 3 giây. */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void mockSend() {
        int n = repo.markPendingSent(OffsetDateTime.now(Texts.VN));
        if (n > 0) log.info("Mock SMS gateway: sent {} message(s)", n);
    }

    @Transactional(readOnly = true)
    public List<SmsView> latest(String phone, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        return (phone == null ? repo.findLatestSms(page) : repo.findLatestSmsByPhone(phone, page)).stream().map(this::toView).toList();
    }

    @SuppressWarnings("unchecked")
    private SmsView toView(OutboxMessage m) {
        Map<String, Object> p = json.readValue(m.getPayload(), Map.class);
        return new SmsView(m.getId(), m.getMessageType(), (String) p.get("phone"), (String) p.get("text"), m.getStatus().name(),
                m.getCreatedAt(), m.getSentAt());
    }
}
