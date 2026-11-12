package vn.sofitech.hmm.admin.repository;

import vn.sofitech.hmm.admin.domain.PosTerminal;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PosTerminalRepository extends JpaRepository<PosTerminal, Long> {

    @EntityGraph(attributePaths = "store")
    List<PosTerminal> findAllByStatusOrderByTerminalCode(String status);

    @EntityGraph(attributePaths = "store")
    Optional<PosTerminal> findByTerminalCodeAndStatus(String terminalCode, String status);

    @EntityGraph(attributePaths = "store")
    Optional<PosTerminal> findByIdAndStatus(Long id, String status);

    long countByStoreId(Long storeId);
}
