package cz.inqool.eas.common.alog.syslog;

import cz.inqool.eas.common.alog.event.Event;
import cz.inqool.eas.common.domain.event.CreateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

public class SyslogObserver {
    private SyslogService service;

    /**
     * Asynchronously sends the event to syslog.
     * @param event Received event
     */
    @Async
    @EventListener
    public void handleEventCreate(CreateEvent<Event> event) {
        service.sendEvent(event.getPayload());
    }

    @Autowired
    public void setService(SyslogService service) {
        this.service = service;
    }
}
