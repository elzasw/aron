package cz.inqool.eas.common.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;

/**
 * Configures {@link RestTemplate}.
 * fixme: rework to send session cookie instead
 */
@Configuration
public class RestConfiguration {
    private HttpServletRequest request;

    /**
     * Constructs {@link RestTemplate} which will forward authorization header from incoming request.
     */
    @Bean("forwardingRestTemplate")
    public RestTemplate forwardingRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.getInterceptors().add((req, body, execution) -> {
            String authorization = request.getHeader("Authorization");
            req.getHeaders().add("Authorization", authorization);

            return execution.execute(req, body);
        });
        return template;
    }

    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Autowired
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }
}
