package lk.ijse.poweralert.controller;

import lk.ijse.poweralert.dto.NotificationDTO;
import lk.ijse.poweralert.dto.ResponseDTO;
import lk.ijse.poweralert.entity.Notification;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.enums.AppEnums.NotificationStatus;
import lk.ijse.poweralert.repository.NotificationRepository;
import lk.ijse.poweralert.service.NotificationService;
import lk.ijse.poweralert.service.UserService;
import lk.ijse.poweralert.util.VarList;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Controller for handling notification-related endpoints
 */
@RestController
@RequestMapping("/api")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ResponseDTO responseDTO;

    /**
     * Get all notifications for the current user
     */
    @GetMapping("/user/notifications")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> getUserNotifications() {
        try {
            logger.info("Fetching notifications for current user");

            // Get current user
            User user = getCurrentUser();

            // Get notifications
            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

            // Convert to DTOs
            List<NotificationDTO> notificationDTOs = notifications.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Notifications retrieved successfully");
            responseDTO.setData(notificationDTOs);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving notifications: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Mark a notification as read
     */
    @PutMapping("/user/notifications/{id}/read")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> markNotificationAsRead(@PathVariable Long id) {
        try {
            logger.info("Marking notification with ID: {} as read", id);

            // Get current user
            User user = getCurrentUser();

            // Find notification
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + id));

            // Verify ownership
            if (!notification.getUser().getId().equals(user.getId())) {
                logger.warn("User {} attempted to access notification {} belonging to user {}",
                        user.getId(), id, notification.getUser().getId());

                responseDTO.setCode(VarList.Forbidden);
                responseDTO.setMessage("You don't have permission to access this notification");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.FORBIDDEN);
            }

            // Update status to DELIVERED if it was SENT
            if (notification.getStatus() == NotificationStatus.SENT) {
                notification.setStatus(NotificationStatus.DELIVERED);
                notification.setDeliveredAt(LocalDateTime.now());
                notification = notificationRepository.save(notification);

                logger.info("Notification {} marked as delivered", id);
            }

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Notification marked as read");
            responseDTO.setData(convertToDTO(notification));

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            logger.error("Notification not found: {}", e.getMessage());

            responseDTO.setCode(VarList.Not_Found);
            responseDTO.setMessage(e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error marking notification as read: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/user/notifications/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> deleteNotification(@PathVariable Long id) {
        try {
            logger.info("Deleting notification with ID: {}", id);

            // Get current user
            User user = getCurrentUser();

            // Find notification
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Notification not found with ID: " + id));

            // Verify ownership
            if (!notification.getUser().getId().equals(user.getId())) {
                logger.warn("User {} attempted to delete notification {} belonging to user {}",
                        user.getId(), id, notification.getUser().getId());

                responseDTO.setCode(VarList.Forbidden);
                responseDTO.setMessage("You don't have permission to delete this notification");
                responseDTO.setData(null);
                return new ResponseEntity<>(responseDTO, HttpStatus.FORBIDDEN);
            }

            // Delete notification
            notificationRepository.delete(notification);
            logger.info("Notification {} deleted successfully", id);

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Notification deleted successfully");
            responseDTO.setData(null);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            logger.error("Notification not found: {}", e.getMessage());

            responseDTO.setCode(VarList.Not_Found);
            responseDTO.setMessage(e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error deleting notification: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Admin endpoint to send a test notification
     */
    @PostMapping("/admin/test-notification")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ResponseDTO> sendTestNotification(@RequestBody TestNotificationRequest request) {
        try {
            logger.info("Sending test notification to user ID: {}", request.getUserId());

            // Find user
            User user = userService.getUserEntityByUsername(request.getUsername());
            if (user == null) {
                throw new EntityNotFoundException("User not found with username: " + request.getUsername());
            }

            // Send test notification - this method returns CompletableFuture<Boolean>
            CompletableFuture<Boolean> future = notificationService.sendTestNotification(user, request.getMessage());

            // Create an intermediate response
            responseDTO.setCode(VarList.Accepted);
            responseDTO.setMessage("Test notification request submitted");
            Map<String, Object> data = new HashMap<>();
            data.put("status", "PROCESSING");
            data.put("userId", user.getId());
            responseDTO.setData(data);

            // Add a completion handler to log the result
            future.whenComplete((sent, exception) -> {
                if (exception != null) {
                    logger.error("Error sending test notification: {}", exception.getMessage(), exception);
                } else if (sent) {
                    logger.info("Test notification sent successfully to user ID: {}", user.getId());
                } else {
                    logger.warn("Failed to send test notification to user ID: {}", user.getId());
                }
            });

            return new ResponseEntity<>(responseDTO, HttpStatus.ACCEPTED);
        } catch (EntityNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());

            responseDTO.setCode(VarList.Not_Found);
            responseDTO.setMessage(e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            logger.error("Error sending test notification: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get all unread notifications for current user
     */
    @GetMapping("/user/notifications/unread")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> getUnreadNotifications() {
        try {
            logger.info("Fetching unread notifications for current user");

            // Get current user
            User user = getCurrentUser();

            // Get unread notifications (SENT status)
            List<Notification> notifications = notificationRepository.findByUserIdAndStatus(
                    user.getId(), NotificationStatus.SENT);

            // Convert to DTOs
            List<NotificationDTO> notificationDTOs = notifications.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage("Unread notifications retrieved successfully");
            responseDTO.setData(notificationDTOs);

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving unread notifications: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Mark all notifications as read for current user
     */
    @PutMapping("/user/notifications/read-all")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'ROLE_UTILITY_PROVIDER')")
    public ResponseEntity<ResponseDTO> markAllNotificationsAsRead() {
        try {
            logger.info("Marking all notifications as read for current user");

            // Get current user
            User user = getCurrentUser();

            // Get all SENT notifications for the user
            List<Notification> notifications = notificationRepository.findByUserIdAndStatus(
                    user.getId(), NotificationStatus.SENT);

            // Update all to DELIVERED
            LocalDateTime now = LocalDateTime.now();
            for (Notification notification : notifications) {
                notification.setStatus(NotificationStatus.DELIVERED);
                notification.setDeliveredAt(now);
            }

            notificationRepository.saveAll(notifications);
            logger.info("Marked {} notifications as read for user {}", notifications.size(), user.getId());

            responseDTO.setCode(VarList.OK);
            responseDTO.setMessage(notifications.size() + " notifications marked as read");
            responseDTO.setData(Map.of("count", notifications.size()));

            return new ResponseEntity<>(responseDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error marking all notifications as read: {}", e.getMessage(), e);

            responseDTO.setCode(VarList.Internal_Server_Error);
            responseDTO.setMessage("Error: " + e.getMessage());
            responseDTO.setData(null);
            return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the current logged-in user
     * @return the user entity
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userService.getUserEntityByUsername(username);
    }

    /**
     * Convert Notification entity to NotificationDTO
     * @param notification the entity to convert
     * @return the DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        return modelMapper.map(notification, NotificationDTO.class);
    }

    /**
     * Request class for test notification
     */
    static class TestNotificationRequest {
        private String username;
        private Long userId;
        private String message;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}