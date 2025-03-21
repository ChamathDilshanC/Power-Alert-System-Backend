package lk.ijse.poweralert.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import lk.ijse.poweralert.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class PushNotificationServiceImpl implements PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationServiceImpl.class);

    @Value("${app.firebase.config-file:firebase-service-account.json}")
    private String firebaseConfigFile;

    @Value("${app.push.enabled:false}")
    private boolean pushEnabled;

    private FirebaseApp firebaseApp;

    @PostConstruct
    public void initialize() {
        if (!pushEnabled) {
            logger.info("Push notification service is disabled");
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource(firebaseConfigFile);
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            // Check if default app exists
            if (FirebaseApp.getApps().isEmpty()) {
                firebaseApp = FirebaseApp.initializeApp(options);
                logger.info("Firebase application has been initialized");
            } else {
                firebaseApp = FirebaseApp.getInstance();
                logger.info("Firebase application already initialized");
            }
        } catch (IOException e) {
            logger.error("Failed to initialize Firebase", e);
            pushEnabled = false;
        }
    }

    @Override
    @Async
    public boolean sendNotification(String deviceToken, String title, String body, Map<String, String> data) {
        if (!pushEnabled) {
            logger.info("Push notifications are disabled. Would have sent to token: {}, title: {}", deviceToken, title);
            return true;
        }

        if (deviceToken == null || deviceToken.isEmpty()) {
            logger.error("Device token is null or empty");
            return false;
        }

        try {
            Message message = buildMessage(deviceToken, title, body, data);
            String response = FirebaseMessaging.getInstance(firebaseApp).send(message);
            logger.info("Notification sent successfully to token: {}, response: {}", deviceToken, response);
            return true;
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send notification to token: {}", deviceToken, e);
            return false;
        }
    }

    @Override
    @Async
    public boolean sendMulticastNotification(String[] deviceTokens, String title, String body, Map<String, String> data) {
        if (!pushEnabled) {
            logger.info("Push notifications are disabled. Would have sent to {} tokens, title: {}",
                    deviceTokens != null ? deviceTokens.length : 0, title);
            return true;
        }

        if (deviceTokens == null || deviceTokens.length == 0) {
            logger.error("Device tokens array is null or empty");
            return false;
        }

        try {
            List<String> validTokens = new ArrayList<>(Arrays.asList(deviceTokens));

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : new HashMap<>())
                    .addAllTokens(validTokens)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance(firebaseApp).sendMulticast(message);

            logger.info("Multicast notification sent successfully. Success count: {}, Failure count: {}",
                    response.getSuccessCount(), response.getFailureCount());

            return response.getSuccessCount() > 0;
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send multicast notification", e);
            return false;
        }
    }

    @Override
    @Async
    public boolean sendTopicNotification(String topic, String title, String body, Map<String, String> data) {
        if (!pushEnabled) {
            logger.info("Push notifications are disabled. Would have sent to topic: {}, title: {}", topic, title);
            return true;
        }

        if (topic == null || topic.isEmpty()) {
            logger.error("Topic is null or empty");
            return false;
        }

        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data != null ? data : new HashMap<>())
                    .setTopic(topic)
                    .build();

            String response = FirebaseMessaging.getInstance(firebaseApp).send(message);
            logger.info("Topic notification sent successfully to topic: {}, response: {}", topic, response);
            return true;
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send notification to topic: {}", topic, e);
            return false;
        }
    }

    @Override
    public boolean subscribeToTopic(String deviceToken, String topic) {
        if (!pushEnabled) {
            logger.info("Push notifications are disabled. Would have subscribed token: {} to topic: {}", deviceToken, topic);
            return true;
        }

        if (deviceToken == null || deviceToken.isEmpty() || topic == null || topic.isEmpty()) {
            logger.error("Device token or topic is null or empty");
            return false;
        }

        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance(firebaseApp)
                    .subscribeToTopic(Collections.singletonList(deviceToken), topic);

            logger.info("Subscription to topic '{}' successful. Success count: {}, Failure count: {}",
                    topic, response.getSuccessCount(), response.getFailureCount());

            return response.getSuccessCount() > 0;
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to subscribe token to topic: {}", topic, e);
            return false;
        }
    }

    @Override
    public boolean unsubscribeFromTopic(String deviceToken, String topic) {
        if (!pushEnabled) {
            logger.info("Push notifications are disabled. Would have unsubscribed token: {} from topic: {}", deviceToken, topic);
            return true;
        }

        if (deviceToken == null || deviceToken.isEmpty() || topic == null || topic.isEmpty()) {
            logger.error("Device token or topic is null or empty");
            return false;
        }

        try {
            TopicManagementResponse response = FirebaseMessaging.getInstance(firebaseApp)
                    .unsubscribeFromTopic(Collections.singletonList(deviceToken), topic);

            logger.info("Unsubscription from topic '{}' successful. Success count: {}, Failure count: {}",
                    topic, response.getSuccessCount(), response.getFailureCount());

            return response.getSuccessCount() > 0;
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to unsubscribe token from topic: {}", topic, e);
            return false;
        }
    }

    private Message buildMessage(String token, String title, String body, Map<String, String> data) {
        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data != null ? data : new HashMap<>())
                .build();
    }

    // Additional utility methods for handling different types of notifications

    /**
     * Builds an Android-specific notification with additional options
     *
     * @param token the device token
     * @param title the notification title
     * @param body the notification body
     * @param data additional data
     * @param imageUrl optional URL for notification image
     * @return the built Message
     */
    public Message buildAndroidMessage(String token, String title, String body, Map<String, String> data, String imageUrl) {
        AndroidConfig.Builder androidConfigBuilder = AndroidConfig.builder()
                .setTtl(3600 * 1000) // 1 hour in milliseconds
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setIcon("notification_icon")
                        .setColor("#2196F3") // Blue color
                        .setClickAction("OPEN_OUTAGE_DETAILS")
                        .build());

        // Add image if provided
        if (imageUrl != null && !imageUrl.isEmpty()) {
            androidConfigBuilder.setNotification(
                    AndroidNotification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setIcon("notification_icon")
                            .setColor("#2196F3")
                            .setClickAction("OPEN_OUTAGE_DETAILS")
                            .setImage(imageUrl)
                            .build()
            );
        }

        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(androidConfigBuilder.build())
                .putAllData(data != null ? data : new HashMap<>())
                .build();
    }

    /**
     * Builds an iOS-specific notification with additional options
     *
     * @param token the device token
     * @param title the notification title
     * @param body the notification body
     * @param data additional data
     * @param badgeCount optional badge count to show on app icon
     * @return the built Message
     */
    public Message buildIosMessage(String token, String title, String body, Map<String, String> data, Integer badgeCount) {
        ApnsConfig.Builder apnsConfigBuilder = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setCategory("OUTAGE_CATEGORY")
                        .setSound("default")
                        .setContentAvailable(true)
                        .build());

        // Add badge count if provided
        if (badgeCount != null) {
            apnsConfigBuilder.setAps(Aps.builder()
                    .setCategory("OUTAGE_CATEGORY")
                    .setSound("default")
                    .setBadge(badgeCount)
                    .setContentAvailable(true)
                    .build());
        }

        return Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setApnsConfig(apnsConfigBuilder.build())
                .putAllData(data != null ? data : new HashMap<>())
                .build();
    }
}