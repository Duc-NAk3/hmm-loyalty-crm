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
public class SourceRow {

    private String sourceVendor;
    private String sourceCustomerRef;
    private Long batchId;
    private OffsetDateTime importedAt;
    private String storeCode;
}
