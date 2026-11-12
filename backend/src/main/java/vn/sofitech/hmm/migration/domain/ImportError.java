package vn.sofitech.hmm.migration.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

/** Dòng dữ liệu lỗi khi nhập (migration.import_errors); raw_data là JSON dòng gốc. */
@Entity
@Table(schema = "migration", name = "import_errors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "import_error_seq")
    @SequenceGenerator(name = "import_error_seq", schema = "migration", sequenceName = "import_errors_error_id_seq", allocationSize = 50)
    @Column(name = "error_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private ImportBatch batch;

    @Column(name = "row_no", nullable = false)
    private int rowNo;

    @Column(nullable = false)
    private String message;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String rawData;

    public ImportError(ImportBatch batch, int rowNo, String message, String rawData) {
        this.batch = batch;
        this.rowNo = rowNo;
        this.message = message;
        this.rawData = rawData;
    }
}
