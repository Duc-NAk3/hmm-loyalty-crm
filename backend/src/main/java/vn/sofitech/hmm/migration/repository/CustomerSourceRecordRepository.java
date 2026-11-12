package vn.sofitech.hmm.migration.repository;

import vn.sofitech.hmm.migration.domain.CustomerSourceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSourceRecordRepository extends JpaRepository<CustomerSourceRecord, Long> {
}
