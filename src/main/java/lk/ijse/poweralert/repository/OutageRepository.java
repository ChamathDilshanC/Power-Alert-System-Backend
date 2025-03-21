package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.Address;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.enums.AppEnums.OutageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface OutageRepository extends JpaRepository<Outage, Long> {

    /** Find outages by status */
    List<Outage> findByStatusIn(List<OutageStatus> statuses);

    /** Find outages for a specific area */
    List<Outage> findByAffectedAreaIdOrderByStartTimeDesc(Long areaId);

    /** Find upcoming outages for a specific area */
    List<Outage> findByAffectedAreaIdAndStartTimeAfterAndStatusOrderByStartTimeAsc(
            Long areaId, LocalDateTime now, OutageStatus status);

    /** Find outages that should be notified in advance  */
    List<Outage> findByStartTimeBetweenAndStatus(
            LocalDateTime startTimeFrom, LocalDateTime startTimeTo, OutageStatus status);

    /** Find outages that affect the given addresses */
    @Query("SELECT o FROM Outage o " +
            "JOIN o.affectedArea a " +
            "WHERE o.status IN :statuses " +
            "AND EXISTS (SELECT 1 FROM Address addr WHERE addr IN :addresses AND addr.district = a.district) " +
            "ORDER BY o.startTime DESC")
    List<Outage> findOutagesForAddresses(@Param("addresses") List<Address> addresses, @Param("statuses") List<OutageStatus> statuses);

    /** Find outages by utility provider */
    List<Outage> findByUtilityProviderIdOrderByStartTimeDesc(Long providerId);

    /** Count outages by status */
    long countByStatusIn(List<OutageStatus> statuses);

    /** Count outages by type */
    long countByType(OutageType type);

    /** Count outages by area */
    long countByAffectedAreaId(Long areaId);

    /** Get outage counts by month for current year */
    @Query("SELECT MONTH(o.startTime) as month, COUNT(o) as count " +
            "FROM Outage o " +
            "WHERE YEAR(o.startTime) = :year " +
            "GROUP BY MONTH(o.startTime) " +
            "ORDER BY month")
    List<Map<String, Object>> countByMonth(@Param("year") int year);

    /** Get average restoration time (in hours) for completed outages */
    @Query("SELECT AVG(TIMESTAMPDIFF(SECOND, o.startTime, o.actualEndTime)) / 3600.0 " +
            "FROM Outage o " +
            "WHERE o.status = 'COMPLETED' AND o.actualEndTime IS NOT NULL")
    Double getAverageRestorationTime();
}