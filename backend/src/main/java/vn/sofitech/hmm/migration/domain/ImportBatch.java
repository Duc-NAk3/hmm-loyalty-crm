package vn.sofitech.hmm.migration.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** Một lần nhập file dữ liệu từ POS cũ (migration.import_batches). */
@Entity
@Table(schema = "migration", name = "import_batches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportBatch {

    public enum Status { UPLOADED, IMPORTED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "source_vendor", nullable = false)
    private String sourceVendor;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "error_rows")
    private Integer errorRows;

    @Column(name = "imported_by")
    private Long importedBy;

    @Column(name = "imported_at")
    private OffsetDateTime importedAt;

    public ImportBatch(Long storeId, String sourceVendor, String dataType, Long importedBy, OffsetDateTime importedAt) {
        this.storeId = storeId;
        this.sourceVendor = sourceVendor;
        this.dataType = dataType;
        this.filePath = "";
        this.status = Status.UPLOADED;
        this.importedBy = importedBy;
        this.importedAt = importedAt;
    }

    public void complete(int totalRows, int errorRows, String filePath) {
        this.status = Status.IMPORTED;
        this.totalRows = totalRows;
        this.errorRows = errorRows;
        this.filePath = filePath;
    }
}
