package cz.inqool.eas.common.alog;

import cz.inqool.eas.common.alog.event.EventBuilder;
import cz.inqool.eas.common.alog.event.EventRepository;
import cz.inqool.eas.common.alog.event.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for sequence subsystem.
 *
 * If application wants to use sequence subsystem,
 * it needs to extend this class and add {@link Configuration} annotation.
 *
 */
@Slf4j
public abstract class AlogConfiguration {
    /**
     * Constructs {@link EventRepository} bean.
     */
    @Bean
    public EventRepository eventRepository() {
        return new EventRepository();
    }

    /**
     * Constructs {@link EventService} bean.
     */
    @Bean
    public EventService eventService() {
        EventService service = new EventService();
        service.setSource(getSource());

        return service;
    }

    @Bean
    public EventBuilder eventBuilder() {
        return new EventBuilder();
    }

    protected abstract String getSource();
}
