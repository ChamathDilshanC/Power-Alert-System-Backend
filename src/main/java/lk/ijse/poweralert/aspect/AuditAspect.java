package lk.ijse.poweralert.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lk.ijse.poweralert.annotation.Auditable;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.service.AuditLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * Aspect for auditing actions performed by users
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);

    @Autowired
    private AuditLogService auditLogService;

    /**
     * Captures method executions annotated with @Auditable and logs them
     */
    @AfterReturning(
            pointcut = "@annotation(lk.ijse.poweralert.annotation.Auditable)",
            returning = "result")
    public void logAuditEvent(JoinPoint joinPoint, Object result) {
        try {
            // Get method signature and annotation
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Auditable auditableAnnotation = method.getAnnotation(Auditable.class);

            // Get current user ID
            Long userId = getCurrentUserId();
            if (userId == null) {
                logger.warn("Cannot log audit event: User not authenticated");
                return;
            }

            // Get action from annotation
            String action = auditableAnnotation.action();

            // Default entity type and ID to class name and result ID
            String entityType = auditableAnnotation.entityType();
            if (entityType.isEmpty()) {
                entityType = result.getClass().getSimpleName();
            }

            // Try to extract ID from the result
            Long entityId = null;
            if (auditableAnnotation.entityIdField().isEmpty()) {
                // Try to get ID through reflection using common ID field names
                entityId = getIdFromObject(result, "id", "Id", "ID", "entityId");
            } else {
                // Get ID from specified field
                entityId = getIdFromObject(result, auditableAnnotation.entityIdField());
            }

            // Get client IP address
            String ipAddress = getClientIp();

            // Build details
            String details = auditableAnnotation.details();
            if (details.isEmpty()) {
                details = "Method: " + method.getName() + ", Args: " + Arrays.toString(joinPoint.getArgs());
            }

            // Log the audit event
            auditLogService.createAuditLog(userId, action, entityType, entityId, details, ipAddress);
            logger.debug("Audit log created for action: {} on entity: {} with ID: {}", action, entityType, entityId);
        } catch (Exception e) {
            logger.error("Error logging audit event: {}", e.getMessage(), e);
            // Continue execution even if audit logging fails
        }
    }

    /**
     * Get the current user ID from security context
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User) {
                return ((User) principal).getId();
            } else if (principal instanceof String) {
                // Handle user ID differently based on your authentication setup
                try {
                    // This is an example - adjust based on how your authentication is set up
                    return Long.parseLong(authentication.getName());
                } catch (NumberFormatException e) {
                    // Not a numeric ID, we might be using usernames instead
                    logger.debug("User ID is not a numeric value");
                }
            }
            // Try to get from credentials
            Object credentials = authentication.getCredentials();
            if (credentials instanceof Long) {
                return (Long) credentials;
            }
        }
        return null;
    }

    /**
     * Extract ID field from object using reflection
     */
    private Long getIdFromObject(Object obj, String... fieldNames) {
        if (obj == null) {
            return null;
        }

        for (String fieldName : fieldNames) {
            try {
                java.lang.reflect.Field field = getField(obj.getClass(), fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value instanceof Long) {
                        return (Long) value;
                    } else if (value instanceof Integer) {
                        return ((Integer) value).longValue();
                    } else if (value instanceof String) {
                        return Long.parseLong((String) value);
                    }
                }
            } catch (Exception e) {
                // Continue to next field name
                logger.trace("Error getting field {} from object: {}", fieldName, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Get field from class or its superclasses
     */
    private java.lang.reflect.Field getField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Get client IP address
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getHeader("WL-Proxy-Client-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
                return ipAddress;
            }
        } catch (Exception e) {
            logger.warn("Error getting client IP: {}", e.getMessage());
        }
        return "unknown";
    }
}