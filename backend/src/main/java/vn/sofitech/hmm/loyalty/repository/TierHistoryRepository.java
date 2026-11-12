package vn.sofitech.hmm.loyalty.repository;

import vn.sofitech.hmm.loyalty.domain.TierHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TierHistoryRepository extends JpaRepository<TierHistory, Long> {

    /** Lần đổi hạng gần nhất của mỗi khách: [customerId, changedAt]. */
    @Query("select h.customerId, max(h.changedAt) from TierHistory h group by h.customerId")
    List<Object[]> lastChangePerCustomer();
}
