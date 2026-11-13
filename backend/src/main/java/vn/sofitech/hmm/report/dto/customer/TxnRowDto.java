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
public class TxnRowDto {

    private Long transactionId;
    private String receiptNo;
    private OffsetDateTime txnTime;
    private String storeCode;
    private String storeName;
    private String txnType;
    private long netAmount;
    private Integer pointsEarned;
    private Integer pointsRedeemed;
    private String source;
    private long lineCount;
}
