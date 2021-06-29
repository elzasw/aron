package cz.inqool.eas.common.stomp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.session.Session;
import org.springframework.session.web.socket.config.annotation.AbstractSessionWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

/**
 * Configuration for web socket subsystem.
 *
 * If application wants to use sequence subsystem,
 * it needs to extend this class and add {@link Configuration} annotation.
 *
 */
@Slf4j
@EnableWebSocketMessageBroker
public abstract class StompConfiguration extends AbstractSessionWebSocketMessageBrokerConfigurer<Session> {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    protected void configureStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(getEndpoint());
    }

    /**
     * Configure STOMP endpoint
     *
     * @see #registerStompEndpoints(StompEndpointRegistry)
     */
    public abstract String getEndpoint();
}
