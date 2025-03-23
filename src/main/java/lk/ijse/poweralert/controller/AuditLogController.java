package lk.ijse.poweralert.controller;

import lk.ijse.poweralert.dto.AuditLogDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.service.AuditLogService;
import lk.ijse.poweralert.util.VarList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for handling audit log related endpoints
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AuditLogController {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogController.class);

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Get all audit logs with pagination
     */
    @GetMapping
    public ResponseEntity<ResponseDTO> getAllAuditLogs(
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        try {
            logger.info("Fetching all audit logs with filters: action={}, fromDate={}, toDate={}",
                    action, fromDate, toDate);

            Page<AuditLogDTO> auditLogs = auditLogService.getAllAuditLogs(pageable, action, fromDate, toDate);

            Map<String, Object> response = new HashMap<>();
            response.put("logs", auditLogs.getContent());
            response.put("currentPage", auditLogs.getNumber());
            response.put("totalItems", auditLogs.getTotalElements());
            response.put("totalPages", auditLogs.getTotalPages());

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Audit logs retrieved successfully");
            responseDTO.setData(response);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving audit logs: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get audit logs for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ResponseDTO> getAuditLogsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        try {
            logger.info("Fetching audit logs for user ID: {} with filters: action={}, fromDate={}, toDate={}",
                    userId, action, fromDate, toDate);

            Page<AuditLogDTO> auditLogs = auditLogService.getAuditLogsByUser(userId, pageable, action, fromDate, toDate);

            Map<String, Object> response = new HashMap<>();
            response.put("logs", auditLogs.getContent());
            response.put("currentPage", auditLogs.getNumber());
            response.put("totalItems", auditLogs.getTotalElements());
            response.put("totalPages", auditLogs.getTotalPages());

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Audit logs for user retrieved successfully");
            responseDTO.setData(response);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving audit logs for user {}: {}", userId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get audit logs for a specific entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ResponseDTO> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        try {
            logger.info("Fetching audit logs for entity type: {} and ID: {} with filters: action={}, fromDate={}, toDate={}",
                    entityType, entityId, action, fromDate, toDate);

            Page<AuditLogDTO> auditLogs = auditLogService.getAuditLogsByEntity(
                    entityType, entityId, pageable, action, fromDate, toDate);

            Map<String, Object> response = new HashMap<>();
            response.put("logs", auditLogs.getContent());
            response.put("currentPage", auditLogs.getNumber());
            response.put("totalItems", auditLogs.getTotalElements());
            response.put("totalPages", auditLogs.getTotalPages());

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Audit logs for entity retrieved successfully");
            responseDTO.setData(response);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving audit logs for entity type {} and ID {}: {}",
                    entityType, entityId, e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get summary of audit logs (counts by action type)
     */
    @GetMapping("/summary")
    public ResponseEntity<ResponseDTO> getAuditLogsSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        try {
            logger.info("Fetching audit logs summary with date range: fromDate={}, toDate={}",
                    fromDate, toDate);

            Map<String, Object> summary = auditLogService.getAuditLogsSummary(fromDate, toDate);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Audit logs summary retrieved successfully");
            responseDTO.setData(summary);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving audit logs summary: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}