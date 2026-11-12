package vn.sofitech.hmm.migration.repository;

import vn.sofitech.hmm.migration.domain.ImportError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportErrorRepository extends JpaRepository<ImportError, Long> {

    List<ImportError> findByBatchIdOrderByRowNo(Long batchId);
}
