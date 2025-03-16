package lk.ijse.poweralert.service.impl;

import lk.ijse.poweralert.entity.*;
import lk.ijse.poweralert.enums.AppEnums.NotificationStatus;
import lk.ijse.poweralert.enums.AppEnums.NotificationType;
import lk.ijse.poweralert.repository.NotificationRepository;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // For email service
    // @Autowired
    // private EmailService emailService;

    // For SMS service
    // @Autowired
    // private SmsService smsService;

    @Override
    @Async
    public void sendOutageNotifications(Outage outage) {
        logger.info("Sending notifications for new outage ID: {}", outage.getId());

        try {
            // Get affected users based on area
            List<User> affectedUsers = findAffectedUsers(outage);
            logger.info("Found {} affected users for outage ID: {}", affectedUsers.size(), outage.getId());

            // Prepare notifications
            List<Notification> notifications = new ArrayList<>();

            for (User user : affectedUsers) {
                // Get user's notification preferences
                List<NotificationPreference> preferences = user.getNotificationPreferences();

                // Skip if user has no preferences
                if (preferences == null || preferences.isEmpty()) {
                    logger.debug("User {} has no notification preferences, using defaults", user.getId());
                    // Use default notification (email)
                    createAndSendNotification(outage, user, NotificationType.EMAIL,
                            generateOutageMessage(outage, user.getPreferredLanguage()));
                    continue;
                }

                // Check each preference
                for (NotificationPreference pref : preferences) {
                    // Skip if preference is not enabled or not matching outage type
                    if (!pref.isEnabled() || pref.getOutageType() != outage.getType()) {
                        continue;
                    }

                    // Create notification
                    createAndSendNotification(outage, user, pref.getChannelType(),
                            generateOutageMessage(outage, user.getPreferredLanguage()));
                }
            }

        } catch (Exception e) {
            logger.error("Error sending outage notifications: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendOutageUpdateNotifications(Outage outage) {
        logger.info("Sending notifications for updated outage ID: {}", outage.getId());

        try {
            // Get affected users based on area
            List<User> affectedUsers = findAffectedUsers(outage);

            for (User user : affectedUsers) {
                // Get user's notification preferences
                List<NotificationPreference> preferences = user.getNotificationPreferences();

                // Skip if user has no preferences
                if (preferences == null || preferences.isEmpty()) {
                    // Use default notification (email)
                    createAndSendNotification(outage, user, NotificationType.EMAIL,
                            generateOutageUpdateMessage(outage, user.getPreferredLanguage()));
                    continue;
                }

                // Check each preference
                for (NotificationPreference pref : preferences) {
                    // Skip if preference is not enabled, not matching outage type, or updates not enabled
                    if (!pref.isEnabled() || pref.getOutageType() != outage.getType() || !pref.isReceiveUpdates()) {
                        continue;
                    }

                    // Create notification
                    createAndSendNotification(outage, user, pref.getChannelType(),
                            generateOutageUpdateMessage(outage, user.getPreferredLanguage()));
                }
            }

        } catch (Exception e) {
            logger.error("Error sending outage update notifications: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendOutageCancellationNotifications(Outage outage) {
        logger.info("Sending notifications for cancelled outage ID: {}", outage.getId());

        try {
            // Get affected users based on area
            List<User> affectedUsers = findAffectedUsers(outage);

            for (User user : affectedUsers) {
                // Get user's notification preferences
                List<NotificationPreference> preferences = user.getNotificationPreferences();

                // Skip if user has no preferences
                if (preferences == null || preferences.isEmpty()) {
                    // Use default notification (email)
                    createAndSendNotification(outage, user, NotificationType.EMAIL,
                            generateOutageCancellationMessage(outage, user.getPreferredLanguage()));
                    continue;
                }

                // Check each preference
                for (NotificationPreference pref : preferences) {
                    // Skip if preference is not enabled, not matching outage type, or updates not enabled
                    if (!pref.isEnabled() || pref.getOutageType() != outage.getType() || !pref.isReceiveUpdates()) {
                        continue;
                    }

                    // Create notification
                    createAndSendNotification(outage, user, pref.getChannelType(),
                            generateOutageCancellationMessage(outage, user.getPreferredLanguage()));
                }
            }

        } catch (Exception e) {
            logger.error("Error sending outage cancellation notifications: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendOutageRestorationNotifications(Outage outage) {
        logger.info("Sending notifications for restored outage ID: {}", outage.getId());

        try {
            // Get affected users based on area
            List<User> affectedUsers = findAffectedUsers(outage);

            for (User user : affectedUsers) {
                // Get user's notification preferences
                List<NotificationPreference> preferences = user.getNotificationPreferences();

                // Skip if user has no preferences
                if (preferences == null || preferences.isEmpty()) {
                    // Use default notification (email)
                    createAndSendNotification(outage, user, NotificationType.EMAIL,
                            generateOutageRestorationMessage(outage, user.getPreferredLanguage()));
                    continue;
                }

                // Check each preference
                for (NotificationPreference pref : preferences) {
                    // Skip if preference is not enabled, not matching outage type, or restoration not enabled
                    if (!pref.isEnabled() || pref.getOutageType() != outage.getType() || !pref.isReceiveRestoration()) {
                        continue;
                    }

                    // Create notification
                    createAndSendNotification(outage, user, pref.getChannelType(),
                            generateOutageRestorationMessage(outage, user.getPreferredLanguage()));
                }
            }

        } catch (Exception e) {
            logger.error("Error sending outage restoration notifications: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean sendTestNotification(User user, String message) {
        logger.info("Sending test notification to user ID: {}", user.getId());

        try {
            // Create a dummy outage for test notification
            Outage dummyOutage = new Outage();
            dummyOutage.setId(-1L);  // Use -1 to indicate test outage

            // Send test notification
            createAndSendNotification(dummyOutage, user, NotificationType.EMAIL, message);

            return true;
        } catch (Exception e) {
            logger.error("Error sending test notification: {}", e.getMessage(), e);
            return false;
        }
    }

    // Helper method to find users affected by an outage
    private List<User> findAffectedUsers(Outage outage) {
        // For now, simple implementation that gets all users with addresses in the affected area's district
        return userRepository.findUsersInDistrict(outage.getAffectedArea().getDistrict());

        // TODO: When geospatial is implemented, this can be replaced with a more accurate query
        // that finds users whose addresses are within the affected geographical area
    }

    // Helper method to create and send a notification
    private void createAndSendNotification(Outage outage, User user, NotificationType type, String content) {
        logger.debug("Creating notification for user ID: {} via {}", user.getId(), type);

        // Create notification entity
        Notification notification = new Notification();
        notification.setOutage(outage);
        notification.setUser(user);
        notification.setType(type);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setContent(content);
        notification.setLanguage(user.getPreferredLanguage());
        notification.setCreatedAt(LocalDateTime.now());

        // Save notification
        notification = notificationRepository.save(notification);

        // Attempt to send notification
        boolean sent = false;

        try {
            switch (type) {
                case EMAIL:
                    // Uncomment when implemented
                    // sent = emailService.sendEmail(user.getEmail(), "Power Outage Notification", content);
                    logger.info("Would send EMAIL to: {}", user.getEmail());
                    sent = true; // For now, pretend it was sent
                    break;
                case SMS:
                    // Uncomment when implemented
                    // sent = smsService.sendSms(user.getPhoneNumber(), content);
                    logger.info("Would send SMS to: {}", user.getPhoneNumber());
                    sent = true; // For now, pretend it was sent
                    break;
                case PUSH:
                    // TODO: Implement push notification
                    logger.info("Would send PUSH notification to user: {}", user.getId());
                    sent = true; // For now, pretend it was sent
                    break;
                case WHATSAPP:
                    // TODO: Implement WhatsApp notification
                    logger.info("Would send WHATSAPP message to: {}", user.getPhoneNumber());
                    sent = true; // For now, pretend it was sent
                    break;
                default:
                    logger.warn("Unknown notification type: {}", type);
            }

            // Update notification status
            if (sent) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);
            } else {
                notification.setStatus(NotificationStatus.FAILED);
                notificationRepository.save(notification);
            }

        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }

    // Helper methods to generate notification messages
    private String generateOutageMessage(Outage outage, String language) {
        ResourceBundle bundle = getResourceBundle(language);

        String areaName = outage.getAffectedArea().getName();
        String startTime = outage.getStartTime().format(DATE_FORMATTER);
        String endTime = outage.getEstimatedEndTime() != null
                ? outage.getEstimatedEndTime().format(DATE_FORMATTER)
                : bundle.getString("unknown");

        return String.format(
                bundle.getString("outage.new"),
                outage.getType(),
                areaName,
                startTime,
                endTime,
                outage.getReason() != null ? outage.getReason() : bundle.getString("reason.unknown")
        );
    }

    private String generateOutageUpdateMessage(Outage outage, String language) {
        ResourceBundle bundle = getResourceBundle(language);

        String areaName = outage.getAffectedArea().getName();
        String endTime = outage.getEstimatedEndTime() != null
                ? outage.getEstimatedEndTime().format(DATE_FORMATTER)
                : bundle.getString("unknown");

        return String.format(
                bundle.getString("outage.update"),
                outage.getType(),
                areaName,
                outage.getStatus(),
                endTime
        );
    }

    private String generateOutageCancellationMessage(Outage outage, String language) {
        ResourceBundle bundle = getResourceBundle(language);

        String areaName = outage.getAffectedArea().getName();

        return String.format(
                bundle.getString("outage.cancelled"),
                outage.getType(),
                areaName,
                outage.getStartTime().format(DATE_FORMATTER)
        );
    }

    private String generateOutageRestorationMessage(Outage outage, String language) {
        ResourceBundle bundle = getResourceBundle(language);

        String areaName = outage.getAffectedArea().getName();

        return String.format(
                bundle.getString("outage.restored"),
                outage.getType(),
                areaName
        );
    }

    // Helper method to get resource bundle for a language
    private ResourceBundle getResourceBundle(String language) {
        Locale locale;

        switch (language.toLowerCase()) {
            case "si":
                locale = new Locale("si", "LK");
                break;
            case "ta":
                locale = new Locale("ta", "LK");
                break;
            default:
                locale = Locale.ENGLISH;
        }

        return ResourceBundle.getBundle("messages", locale);
    }
}