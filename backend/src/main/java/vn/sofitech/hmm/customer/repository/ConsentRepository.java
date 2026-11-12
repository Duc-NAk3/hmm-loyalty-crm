package vn.sofitech.hmm.customer.repository;

import vn.sofitech.hmm.customer.domain.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, Long> {
}
