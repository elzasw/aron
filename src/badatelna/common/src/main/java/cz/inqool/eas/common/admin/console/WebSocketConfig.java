package cz.inqool.eas.common.admin.console;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import javax.validation.constraints.NotNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Common web socket authentication configuration for EAS web socket services. Based on page <a
 * href="https://dzone.com/articles/build-a-secure-app-using-spring-boot-and-websocket">Build a Secure App Using Spring
 * Boot and WebSockets</a>
 * <p>
 * Should be extended and annotated with {@link Configuration} in microservice.
 *
 * @see <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket">WebSocket</a>
 */
@Slf4j
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public abstract class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(getEndpoint())
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NotNull Message<?> message, @NotNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String username = accessor.getLogin();
                    String password = accessor.getPasscode();

                    accessor.setUser(authenticate(username, password));
                    log.debug("WebSocket user '{}' authenticated.", username);
                }
                return message;
            }
        });
    }

    /**
     * Configure STOMP endpoint
     *
     * @see #registerStompEndpoints(StompEndpointRegistry)
     */
    public abstract String getEndpoint();

    /**
     * Authenticates calling user using given {@code username} and {@code password}.
     *
     * @param username user-providen login username
     * @param password user-providen login password
     * @return authentication token with set authorities
     * @throws AuthenticationException in case the authenication fails
     */
    protected abstract AbstractAuthenticationToken authenticate(String username, String password) throws AuthenticationException;
}
