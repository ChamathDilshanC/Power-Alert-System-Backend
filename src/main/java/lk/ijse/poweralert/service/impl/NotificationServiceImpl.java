package lk.ijse.poweralert.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lk.ijse.poweralert.entity.*;
import lk.ijse.poweralert.enums.AppEnums.NotificationStatus;
import lk.ijse.poweralert.enums.AppEnums.NotificationType;
import lk.ijse.poweralert.repository.NotificationPreferenceRepository;
import lk.ijse.poweralert.repository.NotificationRepository;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private EmailService emailService;

    @Autowired(required = false)
    @Qualifier("vonageSmsServiceImpl")
    private SmsService smsService;

    @Autowired(required = false)
    private WhatsAppService whatsAppService;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @Autowired(required = false)
    private UserDeviceService userDeviceService;

    @Autowired
    private MessageSource messageSource;

    @Override
    @Async
    public void sendOutageNotifications(Outage outage) {
        logger.info("Sending notifications for new outage ID: {}", outage.getId());

        try {
            // Get affected users IDs
            List<Long> affectedUserIds = findAffectedUserIds(outage);
            logger.info("Found {} affected users for outage ID: {}", affectedUserIds.size(), outage.getId());

            // Process each user in their own transaction
            for (Long userId : affectedUserIds) {
                processUserNotification(userId, outage);
            }
        } catch (Exception e) {
            logger.error("Error sending outage notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Find user IDs affected by an outage
     *
     * @param outage The outage
     * @return List of affected user IDs
     */
    private List<Long> findAffectedUserIds(Outage outage) {
        return userRepository.findUsersInDistrict(outage.getAffectedArea().getDistrict())
                .stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    /**
     * Process notifications for a single user with proper transaction handling
     *
     * @param userId The ID of the user to process
     * @param outage The outage to send notifications about
     */
    @Transactional(readOnly = true)
    protected void processUserNotification(Long userId, Outage outage) {
        try {
            // Fetch user within this transaction
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

            // Explicitly fetch notification preferences to avoid lazy loading issues
            List<NotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);

            // Skip if user has no preferences
            if (preferences == null || preferences.isEmpty()) {
                logger.debug("User {} has no notification preferences, using defaults", userId);
                // Use default notification (email)
                createAndSendNotification(outage, user, NotificationType.EMAIL,
                        generateOutageMessage(outage, user.getPreferredLanguage()));
                return;
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

            logger.debug("Successfully processed notifications for user ID: {} for outage ID: {}",
                    userId, outage.getId());
        } catch (Exception e) {
            logger.error("Error processing notification for user ID: {} for outage ID: {}: {}",
                    userId, outage.getId(), e.getMessage(), e);
            // Don't re-throw to allow processing other users
        }
    }

    @Override
    @Async
    public void sendOutageUpdateNotifications(Outage outage) {
        logger.info("Sending notifications for updated outage ID: {}", outage.getId());

        try {
            // Get affected user IDs
            List<Long> affectedUserIds = findAffectedUserIds(outage);
            logger.info("Found {} affected users for outage update ID: {}", affectedUserIds.size(), outage.getId());

            // Process each user separately
            for (Long userId : affectedUserIds) {
                processUserUpdateNotification(userId, outage);
            }
        } catch (Exception e) {
            logger.error("Error sending outage update notifications: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    protected void processUserUpdateNotification(Long userId, Outage outage) {
        try {
            // Fetch user within this transaction
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

            // Explicitly fetch notification preferences
            List<NotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);

            // Skip if user has no preferences
            if (preferences == null || preferences.isEmpty()) {
                // Use default notification (email)
                createAndSendNotification(outage, user, NotificationType.EMAIL,
                        generateOutageUpdateMessage(outage, user.getPreferredLanguage()));
                return;
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

            logger.debug("Successfully processed update notifications for user ID: {} for outage ID: {}",
                    userId, outage.getId());
        } catch (Exception e) {
            logger.error("Error processing update notification for user ID: {} for outage ID: {}: {}",
                    userId, outage.getId(), e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendOutageCancellationNotifications(Outage outage) {
        logger.info("Sending notifications for cancelled outage ID: {}", outage.getId());

        try {
            // Get affected user IDs
            List<Long> affectedUserIds = findAffectedUserIds(outage);
            logger.info("Found {} affected users for outage cancellation ID: {}", affectedUserIds.size(), outage.getId());

            // Process each user separately
            for (Long userId : affectedUserIds) {
                processUserCancellationNotification(userId, outage);
            }
        } catch (Exception e) {
            logger.error("Error sending outage cancellation notifications: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    protected void processUserCancellationNotification(Long userId, Outage outage) {
        try {
            // Fetch user within this transaction
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

            // Explicitly fetch notification preferences
            List<NotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);

            // Skip if user has no preferences
            if (preferences == null || preferences.isEmpty()) {
                // Use default notification (email)
                createAndSendNotification(outage, user, NotificationType.EMAIL,
                        generateOutageCancellationMessage(outage, user.getPreferredLanguage()));
                return;
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

            logger.debug("Successfully processed cancellation notifications for user ID: {} for outage ID: {}",
                    userId, outage.getId());
        } catch (Exception e) {
            logger.error("Error processing cancellation notification for user ID: {} for outage ID: {}: {}",
                    userId, outage.getId(), e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendOutageRestorationNotifications(Outage outage) {
        logger.info("Sending notifications for restored outage ID: {}", outage.getId());

        try {
            // Get affected user IDs
            List<Long> affectedUserIds = findAffectedUserIds(outage);
            logger.info("Found {} affected users for outage restoration ID: {}", affectedUserIds.size(), outage.getId());

            // Process each user separately
            for (Long userId : affectedUserIds) {
                processUserRestorationNotification(userId, outage);
            }
        } catch (Exception e) {
            logger.error("Error sending outage restoration notifications: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    protected void processUserRestorationNotification(Long userId, Outage outage) {
        try {
            // Fetch user within this transaction
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

            // Explicitly fetch notification preferences
            List<NotificationPreference> preferences = notificationPreferenceRepository.findByUserId(userId);

            // Skip if user has no preferences
            if (preferences == null || preferences.isEmpty()) {
                // Use default notification (email)
                createAndSendNotification(outage, user, NotificationType.EMAIL,
                        generateOutageRestorationMessage(outage, user.getPreferredLanguage()));
                return;
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

            logger.debug("Successfully processed restoration notifications for user ID: {} for outage ID: {}",
                    userId, outage.getId());
        } catch (Exception e) {
            logger.error("Error processing restoration notification for user ID: {} for outage ID: {}: {}",
                    userId, outage.getId(), e.getMessage(), e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendTestNotification(User user, String message) {
        logger.info("Sending test notification to user ID: {}", user.getId());

        try {
            // Create a dummy outage for test notification
            Outage dummyOutage = new Outage();
            dummyOutage.setId(-1L);  // Use -1 to indicate test outage

            // Create area for the dummy outage
            Area testArea = new Area();
            testArea.setName("Test Area");
            dummyOutage.setAffectedArea(testArea);

            // Set default type for the test outage
            dummyOutage.setType(lk.ijse.poweralert.enums.AppEnums.OutageType.ELECTRICITY);
            dummyOutage.setStatus(lk.ijse.poweralert.enums.AppEnums.OutageStatus.SCHEDULED);

            // Send test notification via email (always, for testing)
            createAndSendNotification(dummyOutage, user, NotificationType.EMAIL, message);

            // Since email service doesn't return success/failure, we assume success if no exception is thrown

            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            logger.error("Error sending test notification: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    // Helper method to create and send a notification
    private void createAndSendNotification(Outage outage, User user, NotificationType type, String content) {
        logger.debug("Creating notification for user ID: {} via {}", user.getId(), type);

        try {
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
                        // Email service returns void, not boolean
                        emailService.sendEmail(user.getEmail(), getEmailSubject(outage, user.getPreferredLanguage()), content);
                        sent = true; // Assume success since exceptions are caught in the email service
                        break;
                    case SMS:
                        // Uses VonageSmsServiceImpl through the SmsService interface
                        if (smsService != null) {
                            sent = smsService.sendSms(user.getPhoneNumber(), content);
                        } else {
                            logger.warn("SMS service not available");
                            sent = false;
                        }
                        break;
                    case PUSH:
                        // Create a data map for push notification
                        Map<String, String> data = new HashMap<>();
                        data.put("outageId", outage.getId().toString());
                        data.put("type", outage.getType().toString());
                        data.put("status", outage.getStatus().toString());

                        // Get user's device token
                        String deviceToken = getDeviceToken(user);

                        if (pushNotificationService != null && deviceToken != null && !deviceToken.isEmpty()) {
                            sent = pushNotificationService.sendNotification(
                                    deviceToken,
                                    getOutageTitle(outage, user.getPreferredLanguage()),
                                    content,
                                    data);
                        } else {
                            logger.warn("Push notification service or device token not available for user ID: {}", user.getId());
                            sent = false;
                        }
                        break;
                    case WHATSAPP:
                        if (whatsAppService != null) {
                            sent = whatsAppService.sendWhatsAppMessage(user.getPhoneNumber(), content);
                        } else {
                            logger.warn("WhatsApp service not available");
                            sent = false;
                        }
                        break;
                    default:
                        logger.warn("Unknown notification type: {}", type);
                }

                // Update notification status
                updateNotificationStatus(notification, sent);

            } catch (Exception e) {
                logger.error("Error sending notification: {}", e.getMessage(), e);
                updateNotificationStatus(notification, false);
            }
        } catch (Exception e) {
            logger.error("Error creating notification: {}", e.getMessage(), e);
        }
    }

    // Helper method to update notification status
    private void updateNotificationStatus(Notification notification, boolean sent) {
        try {
            if (sent) {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } else {
                notification.setStatus(NotificationStatus.FAILED);
            }
            notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Error updating notification status: {}", e.getMessage(), e);
        }
    }

    // Helper method to get device token for push notifications
    private String getDeviceToken(User user) {
        // Use UserDeviceService if available
        if (userDeviceService != null) {
            try {
                List<String> tokens = userDeviceService.getFcmTokensForUser(user.getId());
                if (tokens != null && !tokens.isEmpty()) {
                    return tokens.get(0); // Return first token
                }
            } catch (Exception e) {
                logger.error("Error getting device token for user {}: {}", user.getId(), e.getMessage());
            }
        }

        // Fallback - no token available
        return null;
    }

    // Helper method to get email subject
    private String getEmailSubject(Outage outage, String language) {
        try {
            Locale locale = getLocale(language);
            String outageType = outage.getType().toString();
            String areaName = outage.getAffectedArea().getName();

            // Try to get message from MessageSource first
            try {
                return messageSource.getMessage("outage.email.subject",
                        new Object[]{outageType, areaName}, locale);
            } catch (NoSuchMessageException e) {
                // Fallback to ResourceBundle if MessageSource fails
                ResourceBundle bundle = getResourceBundle(language);
                return String.format(bundle.getString("outage.email.subject"), outageType, areaName);
            }
        } catch (Exception e) {
            logger.warn("Could not get email subject from resource bundle, using fallback: {}", e.getMessage());
            // Fallback subject if all resource lookups fail
            String outageType = outage.getType().toString();
            String areaName = outage.getAffectedArea().getName();
            return outageType + " Outage Alert - " + areaName;
        }
    }

    // Helper method to get outage title for push notifications
    private String getOutageTitle(Outage outage, String language) {
        try {
            Locale locale = getLocale(language);
            String outageType = outage.getType().toString();

            // Try to get message from MessageSource first
            try {
                return messageSource.getMessage("outage.push.title",
                        new Object[]{outageType}, locale);
            } catch (NoSuchMessageException e) {
                // Fallback to ResourceBundle if MessageSource fails
                ResourceBundle bundle = getResourceBundle(language);
                return String.format(bundle.getString("outage.push.title"), outageType);
            }
        } catch (Exception e) {
            logger.warn("Could not get push title from resource bundle, using fallback: {}", e.getMessage());
            // Fallback title if all resource lookups fail
            String outageType = outage.getType().toString();
            return outageType + " Outage Alert";
        }
    }

    private String generateOutageMessage(Outage outage, String language) {
        try {
            ResourceBundle bundle = getResourceBundle(language);

            String areaName = outage.getAffectedArea().getName();
            String startTime = outage.getStartTime().format(DATE_FORMATTER);
            String endTime = outage.getEstimatedEndTime() != null
                    ? outage.getEstimatedEndTime().format(DATE_FORMATTER)
                    : bundle.getString("unknown");
            String reason = outage.getReason() != null && !outage.getReason().isEmpty()
                    ? outage.getReason()
                    : bundle.getString("reason.unknown");

            // Get template and apply MessageFormat (which handles {0}, {1}, etc. format)
            String template = bundle.getString("outage.new");
            return MessageFormat.format(template,
                    outage.getType().toString(),  // {0}
                    areaName,                     // {1}
                    startTime,                    // {2}
                    endTime,                      // {3}
                    reason                        // {4}
            );
        } catch (Exception e) {
            logger.warn("Could not format message from resource bundle, using fallback: {}", e.getMessage());
            // Fallback to HTML format...
        }
        return language;
    }


    private String generateOutageUpdateMessage(Outage outage, String language) {
        try {
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
        } catch (Exception e) {
            logger.warn("Could not get update message from resource bundle, using fallback: {}", e.getMessage());

            String areaName = outage.getAffectedArea().getName();
            String endTime = outage.getEstimatedEndTime() != null
                    ? outage.getEstimatedEndTime().format(DATE_FORMATTER)
                    : "Unknown";

            return outage.getType() + " outage in " + areaName +
                    " status updated to " + outage.getStatus() +
                    ". Estimated end time: " + endTime;
        }
    }

    private String generateOutageCancellationMessage(Outage outage, String language) {
        try {
            ResourceBundle bundle = getResourceBundle(language);

            String areaName = outage.getAffectedArea().getName();

            return String.format(
                    bundle.getString("outage.cancelled"),
                    outage.getType(),
                    areaName,
                    outage.getStartTime().format(DATE_FORMATTER)
            );
        } catch (Exception e) {
            logger.warn("Could not get cancellation message from resource bundle, using fallback: {}", e.getMessage());

            String areaName = outage.getAffectedArea().getName();
            String startTime = outage.getStartTime().format(DATE_FORMATTER);

            return outage.getType() + " outage in " + areaName +
                    " scheduled for " + startTime + " has been cancelled";
        }
    }

    private String generateOutageRestorationMessage(Outage outage, String language) {
        try {
            ResourceBundle bundle = getResourceBundle(language);

            String areaName = outage.getAffectedArea().getName();

            return String.format(
                    bundle.getString("outage.restored"),
                    outage.getType(),
                    areaName
            );
        } catch (Exception e) {
            logger.warn("Could not get restoration message from resource bundle, using fallback: {}", e.getMessage());

            String areaName = outage.getAffectedArea().getName();

            return outage.getType() + " services in " + areaName + " have been restored";
        }
    }

    // Helper method to get resource bundle for a language
    private ResourceBundle getResourceBundle(String language) {
        Locale locale = getLocale(language);

        try {
            return ResourceBundle.getBundle("messages", locale);
        } catch (MissingResourceException e) {
            logger.warn("Could not find resource bundle for locale {}, trying default", locale);
            return ResourceBundle.getBundle("messages", Locale.ENGLISH);
        }
    }

    // Helper method to get locale from language code
    private Locale getLocale(String language) {
        if (language == null) {
            return Locale.ENGLISH;
        }

        switch (language.toLowerCase()) {
            case "si":
                return new Locale("si", "LK");
            case "ta":
                return new Locale("ta", "LK");
            default:
                return Locale.ENGLISH;
        }
    }
}