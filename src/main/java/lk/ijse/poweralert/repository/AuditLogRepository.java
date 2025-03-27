package lk.ijse.poweralert.repository;

import lk.ijse.poweralert.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Find audit logs by user ID
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return paginated audit logs for the user
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    /**
     * Find audit logs by entity type and ID
     *
     * @param entityType the entity type
     * @param entityId the entity ID
     * @param pageable pagination information
     * @return paginated audit logs for the entity
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    /**
     * Find audit logs by action type
     *
     * @param action the action type
     * @param pageable pagination information
     * @return paginated audit logs for the action
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Find audit logs within a date range
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param pageable pagination information
     * @return paginated audit logs within the date range
     */
    Page<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Count audit logs by action
     *
     * @return list of count by action
     */
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a GROUP BY a.action")
    List<Object[]> countByAction();

    /**
     * Count audit logs by entity type
     *
     * @return list of count by entity type
     */
    @Query("SELECT a.entityType, COUNT(a) FROM AuditLog a GROUP BY a.entityType")
    List<Object[]> countByEntityType();

    /**
     * Count audit logs by day within a date range
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of count by day
     */
    @Query("SELECT FUNCTION('DATE', a.timestamp) as day, COUNT(a) FROM AuditLog a " +
            "WHERE a.timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY FUNCTION('DATE', a.timestamp)")
    List<Object[]> countByDay(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count unique users who have audit logs
     *
     * @return count of unique users
     */
    @Query("SELECT COUNT(DISTINCT a.user.id) FROM AuditLog a")
    Long countUniqueUsers();
}