package vn.sofitech.hmm.admin.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = {"role", "store"})
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"role", "store"})
    List<AppUser> findAllByOrderByRoleIdAscUsernameAsc();
}
