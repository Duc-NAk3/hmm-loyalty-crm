package vn.sofitech.hmm.loyalty.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PointRedemptionRepository extends JpaRepository<PointRedemption, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PointRedemption r where r.id = :id")
    Optional<PointRedemption> findForUpdate(@Param("id") Long id);

    List<PointRedemption> findByCustomerIdAndTerminalIdAndStatus(Long customerId, Long terminalId, PointRedemption.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PointRedemption> findByStatusAndHoldExpiresAtBefore(PointRedemption.Status status, OffsetDateTime at);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PointRedemption> findByStatusAndCreatedAtBefore(PointRedemption.Status status, OffsetDateTime at);
}
