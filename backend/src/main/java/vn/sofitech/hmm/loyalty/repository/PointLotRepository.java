package vn.sofitech.hmm.loyalty.repository;

import vn.sofitech.hmm.loyalty.domain.PointLot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface PointLotRepository extends JpaRepository<PointLot, Long> {

    /** Lô còn điểm và còn hạn, cũ nhất trước (FIFO); khóa dòng khi trừ điểm. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from PointLot l
            where l.customerId = :customerId and l.pointsRemaining > 0 and l.expiresAt > :now
            order by l.earnedAt, l.id
            """)
    List<PointLot> findOpenLotsForUpdate(@Param("customerId") Long customerId, @Param("now") OffsetDateTime now);

    /** Tổng điểm sắp hết hạn trong khoảng (now, until]. */
    @Query("""
            select coalesce(sum(l.pointsRemaining), 0) from PointLot l
            where l.customerId = :customerId and l.pointsRemaining > 0 and l.expiresAt > :now and l.expiresAt <= :until
            """)
    long sumExpiring(@Param("customerId") Long customerId, @Param("now") OffsetDateTime now, @Param("until") OffsetDateTime until);

    /** Ngày hết hạn gần nhất của điểm sắp hết hạn. */
    @Query("""
            select min(l.expiresAt) from PointLot l
            where l.customerId = :customerId and l.pointsRemaining > 0 and l.expiresAt > :now and l.expiresAt <= :until
            """)
    OffsetDateTime firstExpiry(@Param("customerId") Long customerId, @Param("now") OffsetDateTime now, @Param("until") OffsetDateTime until);

    List<PointLot> findByCustomerIdOrderByEarnedAt(Long customerId);
}
