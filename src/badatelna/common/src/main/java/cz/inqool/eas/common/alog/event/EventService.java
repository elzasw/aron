package cz.inqool.eas.common.alog.event;

import cz.inqool.eas.common.authored.AuthoredService;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * CRUD service for audit log events.
 */
public class EventService extends AuthoredService<
        Event,
        EventDetail,
        EventList,
        EventCreate,
        EventUpdate,
        EventRepository
        > {

    @Setter
    private String source;

    @Override
    protected void preCreateHook(@NotNull Event event) {
        super.preCreateHook(event);
        event.setSource(source);
    }
}
