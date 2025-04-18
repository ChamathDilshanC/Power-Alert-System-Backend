package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.UtilityProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UtilityProviderRepository extends JpaRepository<UtilityProvider, Long> {
    // In UtilityProviderRepository.java
    @Query("SELECT p FROM UtilityProvider p LEFT JOIN FETCH p.serviceAreas")
    List<UtilityProvider> findAllWithServiceAreas();

}