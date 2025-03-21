package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.UtilityProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilityProviderRepository extends JpaRepository<UtilityProvider, Long> {

}