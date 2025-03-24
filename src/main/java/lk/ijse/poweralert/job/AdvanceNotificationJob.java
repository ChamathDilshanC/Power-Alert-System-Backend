package lk.ijse.poweralert.job;

import lk.ijse.poweralert.entity.NotificationPreference;
import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import lk.ijse.poweralert.enums.AppEnums.NotificationStatus;
import lk.ijse.poweralert.enums.AppEnums.NotificationType;
import lk.ijse.poweralert.enums.AppEnums.OutageStatus;
import lk.ijse.poweralert.repository.NotificationPreferenceRepository;
import lk.ijse.poweralert.repository.NotificationRepository;
import lk.ijse.poweralert.repository.OutageRepository;
import lk.ijse.poweralert.repository.UserRepository;
import lk.ijse.poweralert.service.EmailService;
import lk.ijse.poweralert.service.PushNotificationService;
import lk.ijse.poweralert.service.SmsService;
import lk.ijse.poweralert.service.UserDeviceService;
import lk.ijse.poweralert.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Scheduled job to send advance notifications for upcoming outages
 */
@Component
public class AdvanceNotificationJob {

    private static final Logger logger = LoggerFactory.getLogger(AdvanceNotificationJob.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private OutageRepository outageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private EmailService emailService;

    @Autowired(required = false)
    private SmsService smsService;

    @Autowired(required = false)
    private WhatsAppService whatsAppService;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @Autowired(required = false)
    private UserDeviceService userDeviceService;

    /**
     * Run this job every hour to check for upcoming outages and send advance notifications
     * The cron expression "0 0 * * * *" means at minute 0 of every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(readOnly = true)
    public void sendAdvanceNotifications() {
        logger.info("Starting scheduled job to send advance notifications for upcoming outages");

        try {
            // Look ahead window for outages (next 48 hours)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lookAheadLimit = now.plusHours(48);

            // Find all scheduled outages that start in the look ahead window
            List<Outage> upcomingOutages = outageRepository.findByStartTimeBetweenAndStatus(
                    now, lookAheadLimit, OutageStatus.SCHEDULED);

            logger.info("Found {} upcoming outages within the next 48 hours", upcomingOutages.size());

            for (Outage outage : upcomingOutages) {
                processAdvanceNotifications(outage);
            }

            logger.info("Completed advance notifications job");
        } catch (Exception e) {
            logger.error("Error running advance notifications job: {}", e.getMessage(), e);
        }
    }

    /**
     * Process advance notifications for a specific outage
     * @param outage The upcoming outage
     */
    private void processAdvanceNotifications(Outage outage) {
        try {
            logger.info("Processing advance notifications for outage ID: {} starting at {}",
                    outage.getId(), outage.getStartTime());

            // Get time until outage starts
            LocalDateTime now = LocalDateTime.now();
            long minutesUntilStart = ChronoUnit.MINUTES.between(now, outage.getStartTime());
            long hoursUntilStart = minutesUntilStart / 60;

            logger.info("Outage ID: {} starts in {} hours ({} minutes)",
                    outage.getId(), hoursUntilStart, minutesUntilStart);

            // Find all users who should be notified about this outage
            // This depends on the geographical area of the outage and user addresses
            List<User> affectedUsers = findAffectedUsers(outage);
            logger.info("Found {} affected users for outage ID: {}", affectedUsers.size(), outage.getId());

            // For each affected user, check their notification preferences
            for (User user : affectedUsers) {
                processUserAdvanceNotifications(user, outage, minutesUntilStart);
            }
        } catch (Exception e) {
            logger.error("Error processing advance notifications for outage ID {}: {}",
                    outage.getId(), e.getMessage(), e);
        }
    }

    /**
     * Find users affected by an outage based on their addresses
     * @param outage The outage
     * @return List of affected users
     */
    private List<User> findAffectedUsers(Outage outage) {
        // For now, find users by district matching the affected area's district
        // In a production system, use more sophisticated geospatial queries
        return userRepository.findUsersInDistrict(outage.getAffectedArea().getDistrict());
    }

    /**
     * Process advance notifications for a specific user and outage
     * @param user The user
     * @param outage The outage
     * @param minutesUntilStart Minutes until the outage starts
     */
    private void processUserAdvanceNotifications(User user, Outage outage, long minutesUntilStart) {
        try {
            logger.debug("Processing advance notifications for user ID: {} for outage ID: {}",
                    user.getId(), outage.getId());

            // Get user's notification preferences
            List<NotificationPreference> preferences = notificationPreferenceRepository.findByUserId(user.getId());

            if (preferences.isEmpty()) {
                // If no preferences, use default (email notification 24 hours before)
                if (minutesUntilStart <= 24 * 60 && minutesUntilStart > 23 * 60) {
                    sendAdvanceNotification(user, outage, NotificationType.EMAIL, (int) minutesUntilStart);
                }
                return;
            }

            // Check each preference to see if it matches the outage type and advance notice window
            for (NotificationPreference pref : preferences) {
                // Skip if preference is not enabled or outage type doesn't match
                if (!pref.isEnabled() || pref.getOutageType() != outage.getType()) {
                    continue;
                }

                // Check if we're in the advance notice window for this preference
                // For example, if advanceNoticeMinutes is 60, notify when outage is about 60 mins away
                // We'll give a small window of +/- 5 minutes to account for job scheduling frequency
                int desiredMinutes = pref.getAdvanceNoticeMinutes();
                int lowerBound = desiredMinutes - 5;
                int upperBound = desiredMinutes + 5;

                if (minutesUntilStart <= upperBound && minutesUntilStart > lowerBound) {
                    // We're in the notification window for this preference
                    sendAdvanceNotification(user, outage, pref.getChannelType(), desiredMinutes);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing advance notifications for user ID: {} for outage ID: {}: {}",
                    user.getId(), outage.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send an advance notification to a user about an upcoming outage
     * @param user The user
     * @param outage The outage
     * @param notificationType The notification channel
     * @param minutesBeforeStart The notification window in minutes
     */
    private void sendAdvanceNotification(User user, Outage outage, NotificationType notificationType, int minutesBeforeStart) {
        try {
            logger.info("Sending {} advance notification to user ID: {} for outage ID: {} ({} minutes before)",
                    notificationType, user.getId(), outage.getId(), minutesBeforeStart);

            String subject = "Advance Notice: " + outage.getType() + " Outage in " + outage.getAffectedArea().getName();
            String content = generateAdvanceNotificationContent(user, outage, minutesBeforeStart, user.getPreferredLanguage());

            // First, check if this notification has already been sent
            boolean alreadySent = notificationRepository.existsByOutageIdAndUserIdAndTypeAndStatus(
                    outage.getId(), user.getId(), notificationType, NotificationStatus.SENT);

            if (alreadySent) {
                logger.info("Advance notification for outage ID: {} has already been sent to user ID: {}",
                        outage.getId(), user.getId());
                return;
            }

            // Create notification entity in database
            lk.ijse.poweralert.entity.Notification notification = new lk.ijse.poweralert.entity.Notification();
            notification.setOutage(outage);
            notification.setUser(user);
            notification.setType(notificationType);
            notification.setStatus(NotificationStatus.PENDING);
            notification.setContent(content);
            notification.setLanguage(user.getPreferredLanguage());
            notification.setCreatedAt(LocalDateTime.now());

            notification = notificationRepository.save(notification);

            // Send notification based on type
            boolean sent = false;

            switch (notificationType) {
                case EMAIL:
                    // Prepare model for template
                    Map<String, Object> templateModel = createEmailTemplateModel(user, outage, minutesBeforeStart);

                    // Send email via template asynchronously
                    CompletableFuture<Boolean> emailFuture = emailService.sendTemplateEmail(
                            user.getEmail(),
                            subject,
                            "outage-advance-notice",
                            templateModel
                    );

                    // Handle the result asynchronously
                    lk.ijse.poweralert.entity.Notification finalNotification = notification;
                    lk.ijse.poweralert.entity.Notification finalNotification1 = notification;
                    emailFuture.thenAccept(success -> {
                        if (success) {
                            updateNotificationStatus(finalNotification1.getId(), NotificationStatus.SENT);
                        } else {
                            updateNotificationStatus(finalNotification1.getId(), NotificationStatus.FAILED);
                        }
                    }).exceptionally(ex -> {
                        logger.error("Error sending advance notice email: {}", ex.getMessage());
                        updateNotificationStatus(finalNotification.getId(), NotificationStatus.FAILED);
                        return null;
                    });

                    // For now, assume it will be sent successfully
                    sent = true;
                    break;

                case SMS:
                    if (smsService != null) {
                        sent = smsService.sendSms(user.getPhoneNumber(), content);
                    } else {
                        logger.warn("SMS service not available");
                        sent = false;
                    }
                    break;

                case PUSH:
                    if (pushNotificationService != null && userDeviceService != null) {
                        // Get user's FCM tokens
                        List<String> fcmTokens = userDeviceService.getFcmTokensForUser(user.getId());

                        if (fcmTokens != null && !fcmTokens.isEmpty()) {
                            // Create data map for push notification
                            Map<String, String> data = new HashMap<>();
                            data.put("outageId", outage.getId().toString());
                            data.put("type", outage.getType().toString());
                            data.put("status", "UPCOMING");
                            data.put("minutesBeforeStart", String.valueOf(minutesBeforeStart));

                            // Send to all user devices
                            sent = pushNotificationService.sendMulticastNotification(
                                    fcmTokens.toArray(new String[0]),
                                    subject,
                                    createPushNotificationBody(outage, minutesBeforeStart),
                                    data
                            );
                        } else {
                            logger.warn("No FCM tokens found for user ID: {}", user.getId());
                            sent = false;
                        }
                    } else {
                        logger.warn("Push notification service not available");
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
                    logger.warn("Unsupported notification type: {}", notificationType);
                    sent = false;
            }

            // Update notification status if not already handled by async flow
            if (notificationType != NotificationType.EMAIL) {
                if (sent) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentAt(LocalDateTime.now());
                } else {
                    notification.setStatus(NotificationStatus.FAILED);
                }
                notificationRepository.save(notification);
            }

            logger.info("Advance notification {} sent to user ID: {} for outage ID: {}",
                    sent ? "successfully" : "failed to be", user.getId(), outage.getId());

        } catch (Exception e) {
            logger.error("Error sending advance notification to user ID: {} for outage ID: {}: {}",
                    user.getId(), outage.getId(), e.getMessage(), e);
        }
    }

    /**
     * Update notification status after asynchronous operations
     * @param notificationId The notification ID
     * @param status The new status
     */
    @Transactional
    public void updateNotificationStatus(Long notificationId, NotificationStatus status) {
        try {
            notificationRepository.findById(notificationId).ifPresent(notification -> {
                notification.setStatus(status);
                if (status == NotificationStatus.SENT) {
                    notification.setSentAt(LocalDateTime.now());
                }
                notificationRepository.save(notification);
            });
        } catch (Exception e) {
            logger.error("Error updating notification status: {}", e.getMessage(), e);
        }
    }

    /**
     * Generate advance notification content
     * @param user The user
     * @param outage The outage
     * @param minutesBeforeStart Minutes before the outage starts
     * @param language The user's preferred language
     * @return Notification content
     */
    private String generateAdvanceNotificationContent(User user, Outage outage, int minutesBeforeStart, String language) {
        // For demo, this creates plain text content
        // In production, use templates with proper i18n support
        StringBuilder sb = new StringBuilder();

        int hours = minutesBeforeStart / 60;
        int minutes = minutesBeforeStart % 60;

        sb.append("Advance Notice: ").append(outage.getType()).append(" Outage\n\n");
        sb.append("Hello ").append(user.getUsername()).append(",\n\n");
        sb.append("This is an advance notice about an upcoming ").append(outage.getType())
                .append(" outage in ").append(outage.getAffectedArea().getName()).append(".\n\n");

        sb.append("The outage is scheduled to start in ");
        if (hours > 0) {
            sb.append(hours).append(" hour").append(hours > 1 ? "s" : "");
            if (minutes > 0) {
                sb.append(" and ").append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
            }
        } else {
            sb.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        }
        sb.append(".\n\n");

        sb.append("Details:\n");
        sb.append("- Start time: ").append(outage.getStartTime().format(DATE_FORMATTER)).append("\n");
        if (outage.getEstimatedEndTime() != null) {
            sb.append("- Estimated end time: ").append(outage.getEstimatedEndTime().format(DATE_FORMATTER)).append("\n");
        }
        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            sb.append("- Reason: ").append(outage.getReason()).append("\n");
        }

        sb.append("\nPlease plan accordingly. We apologize for any inconvenience.\n\n");
        sb.append("PowerAlert System");

        return sb.toString();
    }

    /**
     * Create email template model
     * @param user The user
     * @param outage The outage
     * @param minutesBeforeStart Minutes before the outage starts
     * @return Template model
     */
    private Map<String, Object> createEmailTemplateModel(User user, Outage outage, int minutesBeforeStart) {
        Map<String, Object> model = new HashMap<>();

        model.put("username", user.getUsername());
        model.put("outageType", outage.getType().toString());
        model.put("areaName", outage.getAffectedArea().getName());
        model.put("status", "UPCOMING");
        model.put("startTime", outage.getStartTime().format(DATE_FORMATTER));

        if (outage.getEstimatedEndTime() != null) {
            model.put("endTime", outage.getEstimatedEndTime().format(DATE_FORMATTER));
        }

        if (outage.getReason() != null && !outage.getReason().isEmpty()) {
            model.put("reason", outage.getReason());
        }

        // Time information
        int hoursUntilStart = minutesBeforeStart / 60;
        int remainingMinutes = minutesBeforeStart % 60;

        model.put("hoursUntilStart", hoursUntilStart);
        model.put("minutesUntilStart", remainingMinutes);
        model.put("totalMinutesUntilStart", minutesBeforeStart);

        String timeUntilStart;
        if (hoursUntilStart > 0) {
            timeUntilStart = hoursUntilStart + " hour" + (hoursUntilStart > 1 ? "s" : "");
            if (remainingMinutes > 0) {
                timeUntilStart += " and " + remainingMinutes + " minute" + (remainingMinutes > 1 ? "s" : "");
            }
        } else {
            timeUntilStart = minutesBeforeStart + " minute" + (minutesBeforeStart > 1 ? "s" : "");
        }
        model.put("timeUntilStart", timeUntilStart);

        // Links and portal information
        model.put("portalUrl", "https://poweralert.lk/outages/" + outage.getId());
        model.put("year", java.time.Year.now().toString());

        return model;
    }

    /**
     * Create push notification body
     * @param outage The outage
     * @param minutesBeforeStart Minutes before the outage starts
     * @return Push notification body
     */
    private String createPushNotificationBody(Outage outage, int minutesBeforeStart) {
        int hours = minutesBeforeStart / 60;
        int minutes = minutesBeforeStart % 60;

        String timeUntilStart;
        if (hours > 0) {
            timeUntilStart = hours + " hour" + (hours > 1 ? "s" : "");
            if (minutes > 0) {
                timeUntilStart += " and " + minutes + " minute" + (minutes > 1 ? "s" : "");
            }
        } else {
            timeUntilStart = minutes + " minute" + (minutes > 1 ? "s" : "");
        }

        return "Upcoming " + outage.getType() + " outage in " + outage.getAffectedArea().getName() +
                " starts in " + timeUntilStart + " at " + outage.getStartTime().format(DATE_FORMATTER);
    }
}