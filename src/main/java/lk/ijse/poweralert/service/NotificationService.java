package lk.ijse.poweralert.service;

import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;

public interface NotificationService {

    /**
     * Send notifications for a new outage to affected users
     * @param outage the outage entity
     */
    void sendOutageNotifications(Outage outage);

    /**
     * Send notifications for an outage update to affected users
     * @param outage the updated outage entity
     */
    void sendOutageUpdateNotifications(Outage outage);

    /**
     * Send notifications for an outage cancellation to affected users
     * @param outage the cancelled outage entity
     */
    void sendOutageCancellationNotifications(Outage outage);

    /**
     * Send notifications for an outage restoration (completed status) to affected users
     * @param outage the restored outage entity
     */
    void sendOutageRestorationNotifications(Outage outage);

    /**
     * Send a test notification to a specific user
     * @param user the user to send the notification to
     * @param message the notification message
     * @return true if the notification was sent successfully
     */
    boolean sendTestNotification(User user, String message);
}