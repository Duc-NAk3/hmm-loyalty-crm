package vn.sofitech.hmm.migration.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    List<ImportBatch> findAllByOrderByIdDesc();

    /** Số bản ghi đã nhập thành công theo lần nhập: [batchId, count]. */
    @Query("select r.batch.id, count(r) from CustomerSourceRecord r group by r.batch.id")
    List<Object[]> countImportedPerBatch();
}
