package vn.sofitech.hmm.migration.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

/** Bản ghi khách gốc từ POS cũ, để truy vết và gộp (migration.customer_source_records). */
@Entity
@Table(schema = "migration", name = "customer_source_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerSourceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "source_record_seq")
    @SequenceGenerator(name = "source_record_seq", schema = "migration", sequenceName = "customer_source_records_source_id_seq", allocationSize = 50)
    @Column(name = "source_id")
    private Long id;

    /** Hồ sơ chuẩn (module customer) – chỉ giữ ID. */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id")
    private ImportBatch batch;

    @Column(name = "source_vendor", nullable = false)
    private String sourceVendor;

    @Column(name = "source_customer_ref")
    private String sourceCustomerRef;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    @ColumnTransformer(write = "cast(? as jsonb)")
    private String rawData;

    public CustomerSourceRecord(Long customerId, ImportBatch batch, String sourceVendor, String sourceCustomerRef, String rawData) {
        this.customerId = customerId;
        this.batch = batch;
        this.sourceVendor = sourceVendor;
        this.sourceCustomerRef = sourceCustomerRef;
        this.rawData = rawData;
    }
}
