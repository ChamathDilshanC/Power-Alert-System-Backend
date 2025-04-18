package lk.ijse.poweralert.service;

import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

/**
 * Service for sending notifications for various outage events
 */
public interface NotificationService {

    /**
     * Send notifications for a new outage
     *
     * @param outage The outage to send notifications for
     */
    void sendOutageNotifications(Outage outage);

    /**
     * Send notifications for an updated outage
     *
     * @param outage The updated outage to send notifications for
     */
    void sendOutageUpdateNotifications(Outage outage);

    @Async
    void sendOutageUpdateNotification(User user, Outage outage);

    /**
     * Send notifications for a cancelled outage
     *
     * @param outage The cancelled outage to send notifications for
     */
    void sendOutageCancellationNotifications(Outage outage);

    /**
     * Send notifications for a restored outage (service restored)
     *
     * @param outage The restored outage to send notifications for
     */
    void sendOutageRestorationNotifications(Outage outage);

    /**
     * Send test notification to a user
     *
     * @param user The user to send the test notification to
     * @param message The test message content
     * @return CompletableFuture containing the success status of the operation
     */
    CompletableFuture<Boolean> sendTestNotification(User user, String message);
}