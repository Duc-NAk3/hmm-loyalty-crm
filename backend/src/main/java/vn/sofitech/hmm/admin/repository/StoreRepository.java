package vn.sofitech.hmm.admin.repository;

import vn.sofitech.hmm.admin.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findAllByOrderByStoreCode();

    Optional<Store> findByStoreCode(String storeCode);
}
