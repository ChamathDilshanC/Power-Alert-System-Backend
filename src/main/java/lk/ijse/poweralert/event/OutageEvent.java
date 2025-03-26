package lk.ijse.poweralert.event;

import lk.ijse.poweralert.entity.Outage;
import org.springframework.context.ApplicationEvent;

/**
 * Base outage event class
 */
abstract class OutageEvent extends ApplicationEvent {
    private final Outage outage;

    public OutageEvent(Object source, Outage outage) {
        super(source);
        this.outage = outage;
    }

    public Outage getOutage() {
        return outage;
    }
}

/**
 * Event for outage creation
 */
class OutageCreatedEvent extends OutageEvent {
    public OutageCreatedEvent(Object source, Outage outage) {
        super(source, outage);
    }
}

/**
 * Event for outage update
 */
class OutageUpdatedEvent extends OutageEvent {
    public OutageUpdatedEvent(Object source, Outage outage) {
        super(source, outage);
    }
}

/**
 * Event for outage cancellation
 */
class OutageCancelledEvent extends OutageEvent {
    public OutageCancelledEvent(Object source, Outage outage) {
        super(source, outage);
    }
}

/**
 * Event for outage restoration (service restored)
 */
class OutageRestorationEvent extends OutageEvent {
    public OutageRestorationEvent(Object source, Outage outage) {
        super(source, outage);
    }
}