package vn.sofitech.hmm.migration.dto.customerimport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchView {

    private Long batchId;
    private String storeCode;
    private String sourceVendor;
    private String dataType;
    private String status;
    private Integer totalRows;
    private Integer errorRows;
    private OffsetDateTime importedAt;
    private String importedBy;
    private long importedRows;
}
