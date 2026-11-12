package vn.sofitech.hmm.pos.repository;

import vn.sofitech.hmm.pos.domain.Payment;
import vn.sofitech.hmm.pos.domain.SalesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalesTransactionRepository extends JpaRepository<SalesTransaction, Long> {

    Optional<SalesTransaction> findByReceiptNo(String receiptNo);

    long countByShiftId(Long shiftId);

    /** Tổng tiền mặt đã thu trong ca (để đối chiếu khi đóng ca). */
    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.transaction.shiftId = :shiftId and p.method = vn.sofitech.hmm.pos.domain.Payment.Method.CASH
            """)
    long sumCashByShift(@Param("shiftId") Long shiftId);
}
