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
    @Qualifier("twilioSmsServiceImpl")
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
                processUserNotification(userId, outage, "outage-notification.ftl", "outage.new");
            }
        } catch (Exception e) {
            logger.error("Error sending outage notifications: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendOutageUpdateNotifications(Outage outage) {
        logger.info("Sending notifications for updated outage ID: {}", outage.getId());

        try {
            // Get affected users IDs
            List<Long> affectedUserIds = findAffectedUserIds(outage);
            logger.info("Found {} affected users for outage update ID: {}", affectedUserIds.size(), outage.getId());

            // Process each user in their own transaction
            for (Long userId : affectedUserIds) {
                processUserNotification(userId, outage, "outage-update.ftl", "outage.update");
            }
        } catch (Exception e) {
            logger.error("Error sending outage update notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Find user IDs affected by an outage
     *
     * @param outage The outage
     * @return List of affected user IDs
     */
    private List<Long> findAffectedUserIds(Outage outage) {
        if (outage.getAffectedArea() == null || outage.getAffectedArea().getDistrict() == null) {
            logger.warn("Outage {} has null affected area or district", outage.getId());
            return Collections.emptyList();
        }

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
     * @param emailTemplate The email template to use
     * @param messageKey The message key for SMS/WhatsApp templates
     */
    @Transactional(readOnly = true)
    protected void processUserNotification(Long userId, Outage outage, String emailTemplate, String messageKey) {
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
                sendDefaultNotification(outage, user, emailTemplate, messageKey);
                return;
            }

            // Flag to track if any channel was used
            boolean anyChannelUsed = false;

            // Check each preference
            for (NotificationPreference pref : preferences) {
                // Skip if preference is not enabled or not matching outage type
                if (!pref.isEnabled() || pref.getOutageType() != outage.getType()) {
                    continue;
                }

                // Create and send notification via preferred channel
                sendNotificationViaChannel(outage, user, pref.getChannelType(), emailTemplate, messageKey);
                anyChannelUsed = true;
            }

            // If no channel was used (no matching preferences), send via default channel
            if (!anyChannelUsed) {
                sendDefaultNotification(outage, user, emailTemplate, messageKey);
            }

            logger.debug("Successfully processed notifications for user ID: {} for outage ID: {}",
                    userId, outage.getId());
        } catch (Exception e) {
            logger.error("Error processing notification for user ID: {} for outage ID: {}: {}",
                    userId, outage.getId(), e.getMessage(), e);
            // Don't re-throw to allow processing other users
        }
    }

    /**
     * Send notification via default channel (email)
     */
    private void sendDefaultNotification(Outage outage, User user, String emailTemplate, String messageKey) {
        Map<String, Object> model = createNotificationModel(outage, user);
        String subject = getEmailSubject(outage, user.getPreferredLanguage());

        emailService.sendTemplateEmail(
                user.getEmail(),
                subject,
                emailTemplate,
                model,
                user.getPreferredLanguage()
        );

        // Create notification record
        createNotificationRecord(outage, user, NotificationType.EMAIL,
                generateOutageMessage(outage, user.getPreferredLanguage(), messageKey));
    }

    /**
     * Send notification via specified channel
     */
    private void sendNotificationViaChannel(Outage outage, User user,
                                            NotificationType channelType,
                                            String emailTemplate,
                                            String messageKey) {
        try {
            String message = generateOutageMessage(outage, user.getPreferredLanguage(), messageKey);

            switch (channelType) {
                case EMAIL:
                    Map<String, Object> model = createNotificationModel(outage, user);
                    String subject = getEmailSubject(outage, user.getPreferredLanguage());

                    CompletableFuture<Boolean> emailFuture = emailService.sendTemplateEmail(
                            user.getEmail(),
                            subject,
                            emailTemplate,
                            model,
                            user.getPreferredLanguage()
                    );

                    // Create notification record now, update status when future completes
                    Notification emailNotification = createNotificationRecord(
                            outage, user, NotificationType.EMAIL, message);

                    emailFuture.thenAccept(success ->
                            updateNotificationStatus(emailNotification, success));
                    break;

// In NotificationServiceImpl.java - just the relevant part:

                case SMS:
                    if (smsService != null && user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                        // For SMS, use templated message with language
                        String[] params = getMessageParams(outage);

                        // Use sendTemplatedSms with explicit language parameter
                        CompletableFuture<Boolean> smsFuture;
                        if (smsService instanceof TwilioSmsServiceImpl) {
                            TwilioSmsServiceImpl twilioSms = (TwilioSmsServiceImpl) smsService;
                            smsFuture = twilioSms.sendTemplatedSms(
                                    user.getPhoneNumber(),
                                    messageKey,
                                    params,
                                    user.getPreferredLanguage() // Pass user's preferred language
                            );
                        } else {
                            // Fall back to regular SMS
                            smsFuture = smsService.sendSms(user.getPhoneNumber(), message);
                        }

                        // Create notification record now, update status when future completes
                        Notification smsNotification = createNotificationRecord(
                                outage, user, NotificationType.SMS, message);

                        smsFuture.thenAccept(success ->
                                updateNotificationStatus(smsNotification, success));
                    } else {
                        logger.warn("Cannot send SMS to user {}: SMS service unavailable or phone number missing",
                                user.getId());
                    }
                    break;

                case WHATSAPP:
                    if (whatsAppService != null && user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                        // For WhatsApp, we use template messages
                        String[] params = getMessageParams(outage);

                        // Use the overloaded method with language parameter
                        CompletableFuture<Boolean> whatsappFuture = whatsAppService.sendTemplateMessage(
                                user.getPhoneNumber(),
                                messageKey,
                                params,
                                user.getPreferredLanguage() // Pass the language explicitly
                        );

                        Notification whatsappNotification = createNotificationRecord(
                                outage, user, NotificationType.WHATSAPP, message);

                        whatsappFuture.thenAccept(success ->
                                updateNotificationStatus(whatsappNotification, success));
                    } else {
                        logger.warn("Cannot send WhatsApp to user {}: WhatsApp service unavailable or phone number missing",
                                user.getId());
                    }
                    break;

                case PUSH:
                    if (pushNotificationService != null && userDeviceService != null) {
                        List<String> deviceTokens = userDeviceService.getFcmTokensForUser(user.getId());

                        if (deviceTokens != null && !deviceTokens.isEmpty()) {
                            Map<String, String> data = createPushNotificationData(outage);

                            // Use the first token (most recent)
                            String title = getOutageTitle(outage, user.getPreferredLanguage());

                            boolean sent = pushNotificationService.sendNotification(
                                    deviceTokens.get(0),
                                    title,
                                    message,
                                    data
                            );

                            createNotificationRecord(outage, user, NotificationType.PUSH, message,
                                    sent ? NotificationStatus.SENT : NotificationStatus.FAILED);
                        } else {
                            logger.warn("No device tokens found for user {}", user.getId());
                        }
                    } else {
                        logger.warn("Push notification service unavailable for user {}", user.getId());
                    }
                    break;

                default:
                    logger.warn("Unknown notification type: {}", channelType);
            }
        } catch (Exception e) {
            logger.error("Error sending notification via {}: {}", channelType, e.getMessage(), e);
            // Create a failed notification record
            createNotificationRecord(outage, user, channelType,
                    "Failed to send notification: " + e.getMessage(), NotificationStatus.FAILED);
        }
    }

    @Override
    @Async
    public void sendOutageUpdateNotification(User user, Outage outage) {
        String language = user.getPreferredLanguage(); // Get user's preferred language

        // For WhatsApp - Now using the overloaded method with language parameter
        String[] params = {outage.getType().toString(), outage.getAffectedArea().getName(),
                outage.getStatus().toString(), outage.getEstimatedEndTime().format(DATE_FORMATTER)};
        whatsAppService.sendTemplateMessage(user.getPhoneNumber(), "outage.update", params, language);

        // For Email - Using the specific outage update method
        emailService.sendOutageUpdateEmail(user, outage, language);
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
                processUserNotification(userId, outage, "outage-cancellation.ftl", "outage.cancelled");
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
            // Get affected user IDs
            List<Long> affectedUserIds = findAffectedUserIds(outage);
            logger.info("Found {} affected users for outage restoration ID: {}", affectedUserIds.size(), outage.getId());

            // Process each user separately
            for (Long userId : affectedUserIds) {
                // Use a different template for restoration notifications
                processUserNotification(userId, outage, "outage-restoration.ftl", "outage.restored");
            }
        } catch (Exception e) {
            logger.error("Error sending outage restoration notifications: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendTestNotification(User user, String message) {
        logger.info("Sending test notification to user ID: {}", user.getId());

        try {
            // Create a dummy outage for test notification
            Outage dummyOutage = createDummyOutage(user);

            // Send test notification via email (for testing)
            Map<String, Object> model = createNotificationModel(dummyOutage, user);
            model.put("testMessage", message);

            CompletableFuture<Boolean> emailFuture = emailService.sendTemplateEmail(
                    user.getEmail(),
                    "PowerAlert Test Notification",
                    "test-notification.ftl",
                    model,
                    user.getPreferredLanguage()
            );

            // If email template doesn't exist, send a simple email
            emailFuture.exceptionally(ex -> {
                logger.warn("Failed to send with template, using simple email: {}", ex.getMessage());
                emailService.sendEmail(
                        user.getEmail(),
                        "PowerAlert Test Notification",
                        "<h1>PowerAlert Test</h1><p>" + message + "</p>"
                );
                return true;
            });

            // Create notification record
            createNotificationRecord(dummyOutage, user, NotificationType.EMAIL, message);

            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            logger.error("Error sending test notification: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Create a dummy outage for testing
     */
    private Outage createDummyOutage(User user) {
        Outage dummyOutage = new Outage();
        dummyOutage.setId(-1L);  // Use -1 to indicate test outage

        // Create area for the dummy outage
        Area testArea = new Area();
        testArea.setName("Test Area");
        dummyOutage.setAffectedArea(testArea);

        // Set some default properties for the test outage
        dummyOutage.setType(lk.ijse.poweralert.enums.AppEnums.OutageType.ELECTRICITY);
        dummyOutage.setStatus(lk.ijse.poweralert.enums.AppEnums.OutageStatus.SCHEDULED);
        dummyOutage.setStartTime(LocalDateTime.now().plusHours(1));
        dummyOutage.setEstimatedEndTime(LocalDateTime.now().plusHours(3));
        dummyOutage.setReason("System Test");

        return dummyOutage;
    }

    /**
     * Create a notification model for templates
     */
    private Map<String, Object> createNotificationModel(Outage outage, User user) {
        Map<String, Object> model = new HashMap<>();

        // User info
        model.put("username", user.getUsername());
        model.put("email", user.getEmail());

        // Outage info
        model.put("outageType", outage.getType().toString());
        model.put("status", outage.getStatus().toString());
        model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));

        if (outage.getEstimatedEndTime() != null) {
            model.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
        }

        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            model.put("reason", outage.getReason());
        }

        if (outage.getAffectedArea() != null) {
            model.put("areaName", outage.getAffectedArea().getName());
        }

        // Additional template data
        model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
        model.put("year", String.valueOf(LocalDateTime.now().getYear()));
        model.put("updatedAt", LocalDateTime.now().format(DATE_FORMATTER));

        // For outage-advance-notice.ftl
        if (outage.getStartTime() != null) {
            long hoursUntilStart = Math.max(1,
                    LocalDateTime.now().until(outage.getStartTime(), java.time.temporal.ChronoUnit.HOURS));
            model.put("hoursUntilStart", String.valueOf(hoursUntilStart));
        }

        return model;
    }

    /**
     * Create notification data for push notifications
     */
    private Map<String, String> createPushNotificationData(Outage outage) {
        Map<String, String> data = new HashMap<>();

        data.put("outageId", outage.getId().toString());
        data.put("outageType", outage.getType().toString());
        data.put("status", outage.getStatus().toString());
        data.put("startTime", outage.getStartTime().format(DATE_FORMATTER));

        if (outage.getEstimatedEndTime() != null) {
            data.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
        }

        if (outage.getAffectedArea() != null) {
            data.put("areaName", outage.getAffectedArea().getName());
        }

        return data;
    }

    /**
     * Get parameters for message templates
     */
    private String[] getMessageParams(Outage outage) {
        String outageType = outage.getType().toString();
        String areaName = outage.getAffectedArea() != null ? outage.getAffectedArea().getName() : "Unknown area";
        String startTime = outage.getStartTime().format(DATE_FORMATTER);
        String endTime = outage.getEstimatedEndTime() != null ?
                outage.getEstimatedEndTime().format(DATE_FORMATTER) : "Unknown";
        String reason = outage.getReason() != null && !outage.getReason().isEmpty() ?
                outage.getReason() : "Scheduled maintenance";

        return new String[] {outageType, areaName, startTime, endTime, reason};
    }

    /**
     * Create a notification record
     */
    private Notification createNotificationRecord(Outage outage, User user,
                                                  NotificationType type, String content) {
        return createNotificationRecord(outage, user, type, content, NotificationStatus.PENDING);
    }

    /**
     * Create a notification record with specified status
     */
    private Notification createNotificationRecord(Outage outage, User user,
                                                  NotificationType type, String content,
                                                  NotificationStatus status) {
        try {
            Notification notification = new Notification();
            notification.setOutage(outage);
            notification.setUser(user);
            notification.setType(type);
            notification.setStatus(status);
            notification.setContent(content);
            notification.setLanguage(user.getPreferredLanguage());
            notification.setCreatedAt(LocalDateTime.now());

            // Set sent time if the status is SENT
            if (status == NotificationStatus.SENT) {
                notification.setSentAt(LocalDateTime.now());
            }

            return notificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Error creating notification record: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Update notification status
     */
    private void updateNotificationStatus(Notification notification, boolean sent) {
        if (notification == null) {
            return;
        }

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

    /**
     * Generate message for outage notification
     */
    private String generateOutageMessage(Outage outage, String language, String messageKey) {
        try {
            ResourceBundle bundle = getResourceBundle(language);

            // Get the outage parameters
            String[] params = getMessageParams(outage);

            // Find the message template
            String template = bundle.getString(messageKey);

            // Use MessageFormat to replace {0}, {1}, etc. with parameters
            return MessageFormat.format(template, (Object[]) params);
        } catch (Exception e) {
            logger.warn("Could not format message from resource bundle for key {}: {}",
                    messageKey, e.getMessage());

            // Generate a fallback message
            return generateFallbackMessage(outage, messageKey);
        }
    }

    /**
     * Generate a fallback message when the resource bundle fails
     */
    private String generateFallbackMessage(Outage outage, String messageKey) {
        String areaName = outage.getAffectedArea() != null ? outage.getAffectedArea().getName() : "your area";
        String outageType = outage.getType().toString();
        String startTime = outage.getStartTime().format(DATE_FORMATTER);
        String endTime = outage.getEstimatedEndTime() != null ?
                outage.getEstimatedEndTime().format(DATE_FORMATTER) : "unknown time";

        // Generate different messages based on message key
        switch (messageKey) {
            case "outage.new":
                return outageType + " outage scheduled in " + areaName +
                        " from " + startTime + " to " + endTime;
            case "outage.update":
                return outageType + " outage in " + areaName +
                        " has been updated. Status: " + outage.getStatus() +
                        ". Estimated end time: " + endTime;
            case "outage.cancelled":
                return outageType + " outage in " + areaName +
                        " scheduled for " + startTime + " has been cancelled";
            case "outage.restored":
                return outageType + " services in " + areaName + " have been restored";
            default:
                return "PowerAlert notification for " + areaName;
        }
    }

    /**
     * Get email subject for outage notifications
     */
    private String getEmailSubject(Outage outage, String language) {
        try {
            Locale locale = getLocale(language);
            String outageType = outage.getType().toString();
            String areaName = outage.getAffectedArea() != null ?
                    outage.getAffectedArea().getName() : "your area";

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
            // Fallback subject
            String outageType = outage.getType().toString();
            String areaName = outage.getAffectedArea() != null ?
                    outage.getAffectedArea().getName() : "your area";
            return outageType + " Outage Alert - " + areaName;
        }
    }

    /**
     * Get title for push notifications
     */
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
            // Fallback title
            String outageType = outage.getType().toString();
            return outageType + " Outage Alert";
        }
    }

    /**
     * Get resource bundle for a language
     */
    private ResourceBundle getResourceBundle(String language) {
        Locale locale = getLocale(language);

        try {
            // First try to get the exact resource bundle for the locale
            return ResourceBundle.getBundle("messages", locale);
        } catch (MissingResourceException e) {
            logger.warn("Could not find resource bundle for locale {}, trying default", locale);
            try {
                // Fallback to English
                return ResourceBundle.getBundle("messages", Locale.ENGLISH);
            } catch (MissingResourceException e2) {
                logger.error("Could not find default resource bundle", e2);
                // Create an empty resource bundle with fallback values
                return new ResourceBundle() {
                    @Override
                    protected Object handleGetObject(String key) {
                        // Return fallback values for known keys
                        switch (key) {
                            case "outage.new":
                                return "{0} outage scheduled in {1} from {2} to {3}. Reason: {4}";
                            case "outage.update":
                                return "{0} outage in {1} status updated to {2}. End time: {3}";
                            case "outage.cancelled":
                                return "{0} outage in {1} scheduled for {2} has been cancelled";
                            case "outage.restored":
                                return "{0} services in {1} have been restored";
                            case "outage.email.subject":
                                return "{0} Outage Alert - {1}";
                            case "outage.push.title":
                                return "{0} Outage Alert";
                            case "unknown":
                                return "Unknown";
                            case "reason.unknown":
                                return "Scheduled maintenance";
                            default:
                                return null;
                        }
                    }

                    @Override
                    public Enumeration<String> getKeys() {
                        return Collections.emptyEnumeration();
                    }
                };
            }
        }
    }

    /**
     * Get locale from language code
     */
    private Locale getLocale(String language) {
        if (language == null || language.isEmpty()) {
            return Locale.ENGLISH;
        }

        switch (language.toLowerCase()) {
            case "si":
                return new Locale("si", "LK");
            case "ta":
                return new Locale("ta", "LK");
            case "en":
                return Locale.ENGLISH;
            default:
                return Locale.ENGLISH;
        }
    }
}