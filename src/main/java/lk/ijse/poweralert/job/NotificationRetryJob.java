package lk.ijse.poweralert.job;

import lk.ijse.poweralert.entity.Notification;
import lk.ijse.poweralert.enums.AppEnums.NotificationStatus;
import lk.ijse.poweralert.repository.NotificationRepository;
import lk.ijse.poweralert.service.EmailService;
import lk.ijse.poweralert.service.PushNotificationService;
import lk.ijse.poweralert.service.SmsService;
import lk.ijse.poweralert.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class NotificationRetryJob {

    private static final Logger logger = LoggerFactory.getLogger(NotificationRetryJob.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Value("${app.notification.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${app.notification.retry.delay-seconds:300}")
    private int retryDelaySeconds;

    /**
     * Scheduled job to retry failed notifications
     * Runs every 5 minutes by default
     */
    @Scheduled(fixedRateString = "${app.notification.retry.schedule-seconds:300}000")
    public void retryFailedNotifications() {
        logger.info("Starting job to retry failed notifications");

        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(retryDelaySeconds);

        List<Notification> failedNotifications = notificationRepository.findByStatusAndCreatedAtAfter(
                NotificationStatus.FAILED, cutoffTime);

        logger.info("Found {} failed notifications to retry", failedNotifications.size());

        for (Notification notification : failedNotifications) {
            try {
                // Skip if we've hit the max retry attempts
                if (getRetryCount(notification) >= maxRetryAttempts) {
                    logger.info("Skipping notification ID: {} as it has reached max retry attempts", notification.getId());
                    continue;
                }

                logger.info("Retrying notification ID: {}, Type: {}, User: {}",
                        notification.getId(), notification.getType(), notification.getUser().getId());

                boolean sent = false;

                // Attempt to send based on notification type
                try {
                    switch (notification.getType()) {
                        case EMAIL:
                            // Email service returns void, so we wrap in try-catch and assume success if no exception
                            emailService.sendEmail(
                                    notification.getUser().getEmail(),
                                    "Power Outage Notification",
                                    notification.getContent());
                            sent = true; // Assume success if no exception thrown
                            break;
                        case SMS:
                            // Get the CompletableFuture and retrieve its result
                            CompletableFuture<Boolean> smsFuture = smsService.sendSms(
                                    notification.getUser().getPhoneNumber(),
                                    notification.getContent());
                            sent = smsFuture.get(); // This will block until the result is available
                            break;
                        case PUSH:
                            // Placeholder for device token retrieval
                            String deviceToken = getDeviceToken(notification.getUser().getId());
                            if (deviceToken != null && !deviceToken.isEmpty()) {
                                Map<String, String> data = new HashMap<>();
                                data.put("outageId", notification.getOutage().getId().toString());
                                data.put("notificationId", notification.getId().toString());
                                sent = pushNotificationService.sendNotification(
                                        deviceToken,
                                        "Power Outage Notification", // Title should be localized and stored or determined dynamically
                                        notification.getContent(),
                                        data);
                            }
                            break;
                        case WHATSAPP:
                            // Get the CompletableFuture and retrieve its result
                            CompletableFuture<Boolean> whatsappFuture = whatsAppService.sendWhatsAppMessage(
                                    notification.getUser().getPhoneNumber(),
                                    notification.getContent());
                            sent = whatsappFuture.get(); // This will block until the result is available
                            break;
                        default:
                            logger.warn("Unknown notification type: {}", notification.getType());
                    }
                } catch (Exception e) {
                    logger.error("Error sending notification: {}", e.getMessage(), e);
                    sent = false;
                }

                // Update notification status
                if (sent) {
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                    logger.info("Successfully retried notification ID: {}", notification.getId());
                } else {
                    // Increment retry count
                    String retryInfo = notification.getContent();
                    int retryCount = getRetryCount(notification) + 1;
                    setRetryCount(notification, retryCount);
                    notificationRepository.save(notification);
                    logger.warn("Failed to retry notification ID: {}, retry count: {}", notification.getId(), retryCount);
                }
            } catch (Exception e) {
                logger.error("Error retrying notification ID: {}", notification.getId(), e);
                // Increment retry count even on exception
                int retryCount = getRetryCount(notification) + 1;
                setRetryCount(notification, retryCount);
                notificationRepository.save(notification);
            }
        }

        logger.info("Completed job to retry failed notifications");
    }


    private int getRetryCount(Notification notification) {
        try {
            String content = notification.getContent();
            if (content != null && content.contains("RETRY_COUNT:")) {
                String[] parts = content.split("RETRY_COUNT:");
                if (parts.length > 1) {
                    String countPart = parts[1].trim().split("\\s+")[0].trim();
                    return Integer.parseInt(countPart);
                }
            }
        } catch (Exception e) {
            logger.warn("Error parsing retry count for notification ID: {}", notification.getId(), e);
        }
        return 0;
    }

    private void setRetryCount(Notification notification, int count) {
        try {
            String content = notification.getContent();
            if (content == null) {
                content = "";
            }

            if (content.contains("RETRY_COUNT:")) {
                // Replace existing retry count
                content = content.replaceAll("RETRY_COUNT:\\s*\\d+", "RETRY_COUNT: " + count);
            } else {
                // Add retry count at the end (hidden from user in actual message)
                content = content + " [RETRY_COUNT: " + count + "]";
            }
            notification.setContent(content);
        } catch (Exception e) {
            logger.warn("Error setting retry count for notification ID: {}", notification.getId(), e);
        }
    }

    // Placeholder method - in a real implementation, you would retrieve the device token from a database
    private String getDeviceToken(Long userId) {
        return null;
    }
}