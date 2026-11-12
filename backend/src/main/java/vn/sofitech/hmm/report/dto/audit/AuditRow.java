package vn.sofitech.hmm.report.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditRow {

    private Long auditId;
    private OffsetDateTime createdAt;
    private String username;
    private String action;
    private String entityType;
    private String entityId;
    private String afterData;
}
