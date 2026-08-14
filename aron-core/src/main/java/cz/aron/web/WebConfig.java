package cz.aron.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web layer layout: the URL root serves the SPA ({@link IndexController} for the
 * enumerated routes; UI assets are plain static resources under
 * {@code classpath:/META-INF/resources/}, served by Spring Boot's default resource
 * handling once the aron-ui module provides them). The old API keeps its published
 * external URLs under {@link #OLD_API_PREFIX} as a mapping prefix - there is no
 * global servlet context path, so subpath deployment is pure configuration
 * (context path or X-Forwarded-Prefix; see IndexController).
 * <p>
 * The SOAP file-transfer endpoint is deliberately NOT under the API prefix: it is
 * an internal service-to-service interface (Transfagent ingest) served under
 * {@code /cxf/*} (see CxfWsConfig) and must not be exposed by the public reverse
 * proxy.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	/**
	 * External prefix of the old (frozen) REST API, published contract of the
	 * previous-generation UI - pinned by OldApiSurfaceTest.
	 */
	public static final String OLD_API_PREFIX = "/api/aron";

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		configurer.addPathPrefix(OLD_API_PREFIX, HandlerTypePredicate.forBasePackage("cz.aron.controller"));
	}

}
