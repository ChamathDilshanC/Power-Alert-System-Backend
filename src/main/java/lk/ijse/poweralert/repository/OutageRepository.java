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

@Repository
public interface OutageRepository extends JpaRepository<Outage, Long> {

    /** Find outages by status */
    List<Outage> findByStatusIn(List<OutageStatus> statuses);

    /** Find outages for a specific area */
    List<Outage> findByAffectedAreaIdOrderByStartTimeDesc(Long areaId);

    /** Find upcoming outages for a specific area */
    List<Outage> findByAffectedAreaIdAndStartTimeAfterAndStatusOrderByStartTimeAsc(
            Long areaId, LocalDateTime now, OutageStatus status);

    /** Find outages that should be notified in advance */
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

    /** Get average restoration time (in hours) for completed outages */
    @Query(value = "SELECT AVG(TIMESTAMPDIFF(SECOND, o.start_time, o.actual_end_time)) / 3600.0 " +
            "FROM outages o " +
            "WHERE o.status = 'COMPLETED' AND o.actual_end_time IS NOT NULL",
            nativeQuery = true)
    Double getAverageRestorationTime();

    /** Count outages by month for a given year */
    @Query(value = "SELECT MONTH(o.start_time) as month, COUNT(*) as count " +
            "FROM outages o " +
            "WHERE YEAR(o.start_time) = :year " +
            "GROUP BY MONTH(o.start_time) " +
            "ORDER BY month",
            nativeQuery = true)
    List<Object[]> countByMonth(@Param("year") int year);

    /** Find completed outages with actual end time */
    @Query("SELECT o FROM Outage o WHERE o.status = 'COMPLETED' AND o.actualEndTime IS NOT NULL")
    List<Outage> findCompletedOutagesWithEndTime();

    /** Find completed outages with actual end time */
    List<Outage> findByStatusAndActualEndTimeIsNotNull(OutageStatus status);
}