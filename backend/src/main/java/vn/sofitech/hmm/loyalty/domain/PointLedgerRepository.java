package vn.sofitech.hmm.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Sổ cái chỉ ghi thêm: chỉ dùng save (insert) và truy vấn; không có thao tác sửa hay xóa trong code. */
public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {

    @Query("select coalesce(sum(g.points), 0) from PointLedger g where g.customerId = :customerId")
    long sumPoints(@Param("customerId") Long customerId);

    List<PointLedger> findByCustomerIdOrderById(Long customerId);
}
