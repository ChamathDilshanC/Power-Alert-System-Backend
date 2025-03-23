package lk.ijse.poweralert.event;

import lk.ijse.poweralert.entity.Outage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventPublisher {
    private final ApplicationEventPublisher publisher;

    @Autowired
    public NotificationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishOutageNotificationEvent(Outage outage) {
        publisher.publishEvent(new OutageNotificationEvent(outage));
    }
}