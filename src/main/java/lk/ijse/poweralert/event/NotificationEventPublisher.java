package lk.ijse.poweralert.event;

import lk.ijse.poweralert.entity.Outage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publisher for outage-related events to trigger notifications
 */
@Component
public class NotificationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventPublisher.class);
    private final ApplicationEventPublisher eventPublisher;

    public NotificationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publish outage created event
     */
    public void publishOutageCreatedEvent(Object source, Outage outage) {
        logger.info("Publishing outage created event for outage ID: {}", outage.getId());
        OutageCreatedEvent event = new OutageCreatedEvent(source, outage);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publish outage updated event
     */
    public void publishOutageUpdatedEvent(Object source, Outage outage) {
        logger.info("Publishing outage updated event for outage ID: {}", outage.getId());
        OutageUpdatedEvent event = new OutageUpdatedEvent(source, outage);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publish outage cancelled event
     */
    public void publishOutageCancelledEvent(Object source, Outage outage) {
        logger.info("Publishing outage cancelled event for outage ID: {}", outage.getId());
        OutageCancelledEvent event = new OutageCancelledEvent(source, outage);
        eventPublisher.publishEvent(event);
    }

    /**
     * Publish outage restoration event
     */
    public void publishOutageRestorationEvent(Object source, Outage outage) {
        logger.info("Publishing outage restoration event for outage ID: {}", outage.getId());
        OutageRestorationEvent event = new OutageRestorationEvent(source, outage);
        eventPublisher.publishEvent(event);
    }
}