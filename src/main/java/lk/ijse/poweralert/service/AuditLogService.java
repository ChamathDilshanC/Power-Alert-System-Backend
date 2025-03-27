package lk.ijse.poweralert.service;

import lk.ijse.poweralert.dto.AuditLogDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Service interface for audit logging functionality
 */
public interface AuditLogService {

    /**
     * Get all audit logs with optional filtering
     *
     * @param pageable pagination information
     * @param action optional filter by action type
     * @param fromDate optional filter from date (format: yyyy-MM-dd)
     * @param toDate optional filter to date (format: yyyy-MM-dd)
     * @return paginated audit logs
     */
    Page<AuditLogDTO> getAllAuditLogs(Pageable pageable, String action, String fromDate, String toDate);

    /**
     * Get audit logs for a specific user
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @param action optional filter by action type
     * @param fromDate optional filter from date (format: yyyy-MM-dd)
     * @param toDate optional filter to date (format: yyyy-MM-dd)
     * @return paginated audit logs for the user
     */
    Page<AuditLogDTO> getAuditLogsByUser(Long userId, Pageable pageable, String action, String fromDate, String toDate);

    /**
     * Get audit logs for a specific entity
     *
     * @param entityType the entity type (e.g., "User", "Outage", "Address")
     * @param entityId the entity ID
     * @param pageable pagination information
     * @param action optional filter by action type
     * @param fromDate optional filter from date (format: yyyy-MM-dd)
     * @param toDate optional filter to date (format: yyyy-MM-dd)
     * @return paginated audit logs for the entity
     */
    Page<AuditLogDTO> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable,
                                           String action, String fromDate, String toDate);

    /**
     * Create a new audit log entry
     *
     * @param userId the user ID
     * @param action the action performed
     * @param entityType the entity type
     * @param entityId the entity ID
     * @param details additional details
     * @param ipAddress the IP address of the client
     */
    void createAuditLog(Long userId, String action, String entityType, Long entityId, String details, String ipAddress);

    /**
     * Get summary statistics for audit logs
     *
     * @param fromDate optional filter from date (format: yyyy-MM-dd)
     * @param toDate optional filter to date (format: yyyy-MM-dd)
     * @return map containing summary statistics
     */
    Map<String, Object> getAuditLogsSummary(String fromDate, String toDate);
}