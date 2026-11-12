package vn.sofitech.hmm.loyalty.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TierRepository extends JpaRepository<Tier, String> {

    List<Tier> findAllByOrderByThresholdSpend();
}
