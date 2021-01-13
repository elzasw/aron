package cz.inqool.eas.common.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.function.Consumer;
import java.util.function.Function;

@ConditionalOnProperty(prefix = "eas.session", name = "redis", havingValue = "true", matchIfMissing = true)
@Component
public class ServiceHub<S extends Session> {
    public static final String IMPERSONATING_SESSION_KEY = "IMPERSONATING_SESSION";

    private SessionRepository<S> sessionRepository;

    private HttpSession currentSession;

    private SessionCookieSerializer cookieSerializer;

    public RestTemplate templateWithSession(String sessionId) {
        RestTemplate template = new RestTemplate();

        template.getInterceptors().add((req, body, execution) -> {
            cookieSerializer.writeCookieToHeaders(sessionId, req.getHeaders());

            return execution.execute(req, body);
        });

        return template;
    }

    /**
     * Wraps function and provides {@link RestTemplate} which will have automatically added session cookie header
     * based on new session created for provided security context.
     *
     * @param securityContext Provided security context
     * @param function Function to wrap
     * @param <T> Type of return statement
     * @return return statement
     */
    public <T> T doInContext(SecurityContext securityContext, Function<RestTemplate, T> function) {
        S session = sessionRepository.createSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        session.setAttribute(IMPERSONATING_SESSION_KEY, true);
        sessionRepository.save(session);

        RestTemplate template = templateWithSession(session.getId());

        try {
            return function.apply(template);
        } finally {
            sessionRepository.deleteById(session.getId());
        }
    }

    /**
     * Wraps function and provides {@link RestTemplate} which will have automatically added session cookie header
     * based on current session.
     *
     * @param function Function to wrap
     * @param <T> Type of return statement
     * @return return statement
     */
    public <T> T doInCurrentContext(Function<RestTemplate, T> function) {
        String sessionId = currentSession.getId();
        RestTemplate template = templateWithSession(sessionId);

        return function.apply(template);
    }

    public void doInContext(SecurityContext securityContext, Consumer<RestTemplate> runnable) {
        doInContext(securityContext, (template) -> {
            runnable.accept(template);
            return 0;
        });
    }


    public void doInCurrentContext(Consumer<RestTemplate> runnable) {
        doInCurrentContext((template -> {
            runnable.accept(template);
            return 0;
        }));
    }


    @Autowired
    public void setSessionRepository(SessionRepository<S> sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Autowired
    public void setCookieSerializer(SessionCookieSerializer cookieSerializer) {
        this.cookieSerializer = cookieSerializer;
    }

    @Autowired(required = false)
    public void setCurrentSession(HttpSession currentSession) {
        this.currentSession = currentSession;
    }
}
