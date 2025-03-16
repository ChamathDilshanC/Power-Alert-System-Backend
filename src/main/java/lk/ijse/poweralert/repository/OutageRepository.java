package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.Address;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutageRepository extends JpaRepository<Outage, Long> {

    /**
     * Find outages by status
     * @param statuses the list of statuses to find
     * @return list of outages with the given statuses
     */
    List<Outage> findByStatusIn(List<OutageStatus> statuses);

    /**
     * Find outages for a specific area
     * @param areaId the area ID
     * @return list of outages for the area
     */
    List<Outage> findByAffectedAreaIdOrderByStartTimeDesc(Long areaId);

    /**
     * Find upcoming outages for a specific area
     * @param areaId the area ID
     * @param now the current time
     * @param status the status to filter by
     * @return list of upcoming outages for the area
     */
    List<Outage> findByAffectedAreaIdAndStartTimeAfterAndStatusOrderByStartTimeAsc(
            Long areaId, LocalDateTime now, OutageStatus status);

    /**
     * Find outages that should be notified in advance
     * @param startTimeFrom the start time lower bound
     * @param startTimeTo the start time upper bound
     * @param status the outage status
     * @return list of outages to be notified
     */
    List<Outage> findByStartTimeBetweenAndStatus(
            LocalDateTime startTimeFrom, LocalDateTime startTimeTo, OutageStatus status);

    /**
     * Find outages that affect the given addresses
     * This query finds outages in districts where the user has addresses
     * @param addresses the list of user addresses
     * @param statuses the list of outage statuses to include
     * @return list of outages affecting the addresses
     */
    @Query("SELECT o FROM Outage o " +
            "JOIN o.affectedArea a " +
            "WHERE o.status IN :statuses " +
            "AND EXISTS (SELECT 1 FROM Address addr WHERE addr IN :addresses AND addr.district = a.district) " +
            "ORDER BY o.startTime DESC")
    List<Outage> findOutagesForAddresses(@Param("addresses") List<Address> addresses, @Param("statuses") List<OutageStatus> statuses);

    /**
     * Find outages by utility provider
     * @param providerId the utility provider ID
     * @return list of outages for the provider
     */
    List<Outage> findByUtilityProviderIdOrderByStartTimeDesc(Long providerId);
}