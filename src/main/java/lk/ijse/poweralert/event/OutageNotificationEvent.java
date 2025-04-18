package lk.ijse.poweralert.event;

import lk.ijse.poweralert.entity.Outage;

public class OutageNotificationEvent {
    private final Outage outage;

    public OutageNotificationEvent(Outage outage) {
        this.outage = outage;
    }

    public Outage getOutage() {
        return outage;
    }
}