package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.dto.AuditLogDTO;
import lk.ijse.poweralert.entity.AuditLog;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.repository.AuditLogRepository;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.AuditLogService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                               UserRepository userRepository,
                               ModelMapper modelMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAllAuditLogs(Pageable pageable, String action, String fromDate, String toDate) {
        logger.debug("Getting all audit logs with action={}, fromDate={}, toDate={}", action, fromDate, toDate);

        Page<AuditLog> auditLogPage = auditLogRepository.findAll(
                createFilterSpecification(null, null, action, fromDate, toDate),
                pageable);

        return auditLogPage.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogsByUser(Long userId, Pageable pageable, String action, String fromDate, String toDate) {
        logger.debug("Getting audit logs for user ID: {} with action={}, fromDate={}, toDate={}",
                userId, action, fromDate, toDate);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with ID: " + userId);
        }

        Page<AuditLog> auditLogPage = auditLogRepository.findAll(
                createFilterSpecification(userId, null, action, fromDate, toDate),
                pageable);

        return auditLogPage.map(this::convertToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDTO> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable,
                                                  String action, String fromDate, String toDate) {
        logger.debug("Getting audit logs for entity type: {} and ID: {} with action={}, fromDate={}, toDate={}",
                entityType, entityId, action, fromDate, toDate);

        // Create entity specification
        Specification<AuditLog> spec = createFilterSpecification(null, entityId, action, fromDate, toDate)
                .and((root, query, cb) -> cb.equal(root.get("entityType"), entityType));

        Page<AuditLog> auditLogPage = auditLogRepository.findAll(spec, pageable);

        return auditLogPage.map(this::convertToDTO);
    }

    @Override
    @Transactional
    public void createAuditLog(Long userId, String action, String entityType, Long entityId, String details, String ipAddress) {
        logger.debug("Creating audit log for user ID: {}, action: {}, entityType: {}, entityId: {}",
                userId, action, entityType, entityId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

            AuditLog auditLog = new AuditLog();
            auditLog.setUser(user);
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDetails(details);
            auditLog.setIpAddress(ipAddress);
            auditLog.setTimestamp(LocalDateTime.now());

            auditLogRepository.save(auditLog);
            logger.debug("Audit log created successfully with ID: {}", auditLog.getId());
        } catch (Exception e) {
            logger.error("Error creating audit log: {}", e.getMessage(), e);
            // Continue even if audit logging fails to avoid disrupting main functionality
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAuditLogsSummary(String fromDate, String toDate) {
        logger.debug("Getting audit logs summary with fromDate={}, toDate={}", fromDate, toDate);

        // Parse date range
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                startDate = LocalDate.parse(fromDate, DATE_FORMATTER).atStartOfDay();
            } catch (DateTimeParseException e) {
                logger.warn("Invalid fromDate format: {}", fromDate);
            }
        }

        if (toDate != null && !toDate.isEmpty()) {
            try {
                endDate = LocalDate.parse(toDate, DATE_FORMATTER).atTime(LocalTime.MAX);
            } catch (DateTimeParseException e) {
                logger.warn("Invalid toDate format: {}", toDate);
            }
        }

        // Create a specification for the date range
        final Specification<AuditLog> spec = createDateRangeSpecification(startDate, endDate);

        // Get all logs in the date range
        List<AuditLog> auditLogs = auditLogRepository.findAll(spec);

        return createSummaryMap(auditLogs);
    }

    /**
     * Create a specification for filtering by date range
     */
    private Specification<AuditLog> createDateRangeSpecification(final LocalDateTime startDate, final LocalDateTime endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create a summary map from audit logs
     */
    private Map<String, Object> createSummaryMap(final List<AuditLog> auditLogs) {
        Map<String, Object> summary = new HashMap<>();

        // Count by action type
        Map<String, Long> actionCounts = auditLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting()));
        summary.put("actionCounts", actionCounts);

        // Count by entity type
        Map<String, Long> entityCounts = auditLogs.stream()
                .collect(Collectors.groupingBy(AuditLog::getEntityType, Collectors.counting()));
        summary.put("entityCounts", entityCounts);

        // Count by day (for chart)
        Map<String, Long> dailyCounts = auditLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getTimestamp().format(DATE_FORMATTER),
                        Collectors.counting()));
        summary.put("dailyCounts", dailyCounts);

        // Total logs
        summary.put("totalLogs", auditLogs.size());

        // Unique users who have logs
        long uniqueUsers = auditLogs.stream()
                .map(log -> log.getUser().getId())
                .distinct()
                .count();
        summary.put("uniqueUsers", uniqueUsers);

        return summary;
    }

    /**
     * Create a specification for filtering audit logs
     */
    private Specification<AuditLog> createFilterSpecification(final Long userId, final Long entityId,
                                                              final String action, final String fromDate,
                                                              final String toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User filter
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            // Entity ID filter
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }

            // Action filter
            if (action != null && !action.isEmpty()) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            // Date range filter
            addDateRangePredicates(predicates, root, cb, fromDate, toDate);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Add date range predicates to the list
     */
    private void addDateRangePredicates(List<Predicate> predicates,
                                        jakarta.persistence.criteria.Root<AuditLog> root,
                                        jakarta.persistence.criteria.CriteriaBuilder cb,
                                        String fromDate, String toDate) {
        if (fromDate != null && !fromDate.isEmpty()) {
            try {
                final LocalDateTime startDate = LocalDate.parse(fromDate, DATE_FORMATTER).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            } catch (DateTimeParseException e) {
                logger.warn("Invalid fromDate format: {}", fromDate);
            }
        }

        if (toDate != null && !toDate.isEmpty()) {
            try {
                final LocalDateTime endDate = LocalDate.parse(toDate, DATE_FORMATTER).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            } catch (DateTimeParseException e) {
                logger.warn("Invalid toDate format: {}", toDate);
            }
        }
    }

    /**
     * Convert AuditLog entity to AuditLogDTO
     */
    private AuditLogDTO convertToDTO(AuditLog auditLog) {
        AuditLogDTO dto = modelMapper.map(auditLog, AuditLogDTO.class);
        dto.setUserId(auditLog.getUser().getId());
        return dto;
    }
}