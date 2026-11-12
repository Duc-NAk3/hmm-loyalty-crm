package vn.sofitech.hmm.report.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LedgerRow {

    private Long ledgerId;
    private OffsetDateTime createdAt;
    private String entryType;
    private int points;
    private int balanceAfter;
    private String receiptNo;
    private Long redemptionId;
    private OffsetDateTime expiresAt;
}
