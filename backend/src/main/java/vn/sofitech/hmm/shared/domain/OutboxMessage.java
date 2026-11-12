package vn.sofitech.hmm.shared.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.OffsetDateTime;

/** "Hộp thư đi" (shared.outbox_messages): việc cần gửi ra ngoài, tác vụ nền gửi và thử lại. */
@Entity
@Table(schema = "shared", name = "outbox_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage {

    public enum Status { PENDING, SENT, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_seq")
    @SequenceGenerator(name = "outbox_seq", schema = "shared", sequenceName = "outbox_messages_outbox_id_seq", allocationSize = 50)
    @Column(name = "outbox_id")
    private Long id;

    @Column(name = "message_type", nullable = false)
    private String messageType;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    /** Nội dung cần gửi (JSON). */
    @Column(nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    public OutboxMessage(String messageType, String aggregateType, Long aggregateId, String payload, OffsetDateTime at) {
        this.messageType = messageType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = Status.PENDING;
        this.nextAttemptAt = at;
        this.createdAt = at;
    }
}
