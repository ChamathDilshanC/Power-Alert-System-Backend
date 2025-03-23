package lk.ijse.poweralert.service;

import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.entity.User;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    /** Send notifications for a new outage to affected users */
    void sendOutageNotifications(Outage outage);

    /** Send notifications for an outage update to affected users */
    void sendOutageUpdateNotifications(Outage outage);

    /** Send notifications for an outage cancellation to affected users */
    void sendOutageCancellationNotifications(Outage outage);

    /** Send notifications for an outage restoration (completed status) to affected users */
    void sendOutageRestorationNotifications(Outage outage);

    /** Send a test notification to a specific user */
    CompletableFuture<Boolean> sendTestNotification(User user, String message);
}