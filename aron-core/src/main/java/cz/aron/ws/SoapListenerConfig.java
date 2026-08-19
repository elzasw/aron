package cz.aron.ws;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Optional separate listener for the internal SOAP interfaces ({@code /cxf/**}).
 * Without {@code soap.port} nothing here exists and the interfaces stay on the main
 * server port, which keeps a plain deployment - and development - free of a second
 * port. With it configured the split is strict, see {@link SoapPortFilter}:
 * {@code /cxf/**} answers ONLY on the SOAP port, and the SOAP port answers nothing
 * else, so a misconfigured reverse proxy can no longer reach the internal
 * interfaces at all.
 * <p>
 * The listener is an additional Tomcat connector rather than a second application:
 * a connector is only a socket into the same servlet context, so both CXF endpoints
 * keep their paths and their wiring. Optional {@code soap.address} binds it to one
 * interface (an internal network), which is the real gain over blocking the paths
 * at the proxy. An unusable address stops the startup - a silently public internal
 * interface would be worse than a failed boot.
 */
@ConditionalOnProperty(name = "soap.port")
@Configuration
public class SoapListenerConfig {

	private static final Logger log = LoggerFactory.getLogger(SoapListenerConfig.class);

	private final Connector connector;

	public SoapListenerConfig(@Value("${soap.port}") int port, @Value("${soap.address:}") String address) {
		connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
		connector.setPort(port);
		if (!address.isBlank()) {
			bind(address);
		}
		log.info("Internal SOAP interfaces get their own listener on port {}{}", port,
				address.isBlank() ? "" : " (address " + address + ")");
	}

	private void bind(String address) {
		if (!(connector.getProtocolHandler() instanceof AbstractProtocol<?> protocol)) {
			throw new IllegalStateException("Cannot bind the SOAP listener to " + address
					+ ": unexpected protocol handler " + connector.getProtocolHandler());
		}
		try {
			protocol.setAddress(InetAddress.getByName(address));
		} catch (UnknownHostException e) {
			throw new IllegalStateException("Unknown soap.address: " + address, e);
		}
	}

	@Bean
	public SoapListener soapListener() {
		return new SoapListener(connector);
	}

	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> soapConnectorCustomizer() {
		return factory -> factory.addAdditionalTomcatConnectors(connector);
	}

	@Bean
	public FilterRegistrationBean<SoapPortFilter> soapPortFilter(SoapListener soapListener) {
		var registration = new FilterRegistrationBean<>(new SoapPortFilter(soapListener));
		registration.addUrlPatterns("/*");
		// as early as possible: no request must reach a surface of the other port
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		return registration;
	}

}
