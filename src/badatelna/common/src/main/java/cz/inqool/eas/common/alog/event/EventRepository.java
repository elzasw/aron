package cz.inqool.eas.common.alog.event;

import cz.inqool.eas.common.authored.AuthoredRepository;
import cz.inqool.eas.common.authored.index.AuthoredIndex;
import cz.inqool.eas.common.authored.store.AuthoredStore;

/**
 * CRUD repository for audit log events.
 */
public class EventRepository extends AuthoredRepository<
        Event,
        Event,
        EventIndexedObject,
        AuthoredStore<Event, Event, QEvent>,
        AuthoredIndex<Event, Event, EventIndexedObject>> {
}
