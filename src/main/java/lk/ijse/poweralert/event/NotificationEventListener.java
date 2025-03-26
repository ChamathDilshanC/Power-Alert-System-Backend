package lk.ijse.poweralert.event;

import lk.ijse.poweralert.entity.Outage;
import lk.ijse.poweralert.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener for outage events to trigger notifications
 */
@Component
public class NotificationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventListener.class);

    @Autowired
    private NotificationService notificationService;

    /**
     * Handle outage created event
     */
    @EventListener
    @Async
    public void handleOutageCreatedEvent(OutageCreatedEvent event) {
        logger.info("Received outage created event for outage ID: {}", event.getOutage().getId());
        notificationService.sendOutageNotifications(event.getOutage());
    }

    /**
     * Handle outage updated event
     */
    @EventListener
    @Async
    public void handleOutageUpdatedEvent(OutageUpdatedEvent event) {
        logger.info("Received outage updated event for outage ID: {}", event.getOutage().getId());
        notificationService.sendOutageUpdateNotifications(event.getOutage());
    }

    /**
     * Handle outage cancelled event
     */
    @EventListener
    @Async
    public void handleOutageCancelledEvent(OutageCancelledEvent event) {
        logger.info("Received outage cancelled event for outage ID: {}", event.getOutage().getId());
        notificationService.sendOutageCancellationNotifications(event.getOutage());
    }

    /**
     * Handle outage restoration event
     */
    @EventListener
    @Async
    public void handleOutageRestorationEvent(OutageRestorationEvent event) {
        logger.info("Received outage restoration event for outage ID: {}", event.getOutage().getId());
        notificationService.sendOutageRestorationNotifications(event.getOutage());
    }
}