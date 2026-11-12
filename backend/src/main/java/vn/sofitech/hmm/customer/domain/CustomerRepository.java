package vn.sofitech.hmm.customer.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhoneAndStatusNot(String phone, Customer.Status status);

    Optional<Customer> findByMemberCodeAndStatusNot(String memberCode, Customer.Status status);

    /** Khóa dòng khi cộng / trừ điểm để hai giao dịch đồng thời không ghi đè số dư. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findForUpdate(@Param("id") Long id);

    List<Customer> findByStatusNotIn(List<Customer.Status> statuses);

    /** Mã thành viên HM + 8 chữ số sinh từ sequence, không đổi (BRL-01). */
    @Query(value = "select nextval('customer.member_code_seq')", nativeQuery = true)
    long nextMemberNumber();
}
