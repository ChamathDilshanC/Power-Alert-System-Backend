package lk.ijse.poweralert.service;

import java.util.Map;

/**
 * Service interface for sending push notifications
 */
public interface PushNotificationService {

    /**
     * Send a push notification to a specific device
     *
     * @param deviceToken the FCM device token
     * @param title the notification title
     * @param body the notification body
     * @param data additional data payload (optional)
     * @return true if the notification was sent successfully, false otherwise
     */
    boolean sendNotification(String deviceToken, String title, String body, Map<String, String> data);

    /**
     * Send a push notification to multiple devices
     *
     * @param deviceTokens array of FCM device tokens
     * @param title the notification title
     * @param body the notification body
     * @param data additional data payload (optional)
     * @return true if the notification was sent successfully to at least one device, false otherwise
     */
    boolean sendMulticastNotification(String[] deviceTokens, String title, String body, Map<String, String> data);

    /**
     * Send a push notification to a topic
     *
     * @param topic the FCM topic name
     * @param title the notification title
     * @param body the notification body
     * @param data additional data payload (optional)
     * @return true if the notification was sent successfully, false otherwise
     */
    boolean sendTopicNotification(String topic, String title, String body, Map<String, String> data);

    /**
     * Subscribe a device to a topic
     *
     * @param deviceToken the FCM device token
     * @param topic the topic name
     * @return true if subscription was successful, false otherwise
     */
    boolean subscribeToTopic(String deviceToken, String topic);

    /**
     * Unsubscribe a device from a topic
     *
     * @param deviceToken the FCM device token
     * @param topic the topic name
     * @return true if unsubscription was successful, false otherwise
     */
    boolean unsubscribeFromTopic(String deviceToken, String topic);
}