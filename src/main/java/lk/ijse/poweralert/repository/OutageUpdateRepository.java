package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.OutageUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutageUpdateRepository extends JpaRepository<OutageUpdate, Long> {

    /**
     * Find updates for a specific outage
     * @param outageId the outage ID
     * @return list of updates for the outage
     */
    List<OutageUpdate> findByOutageIdOrderByCreatedAtDesc(Long outageId);
}