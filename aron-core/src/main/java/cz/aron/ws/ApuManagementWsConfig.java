package cz.aron.ws;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cz.aron.management.v1.AronManagementService;
import jakarta.xml.ws.Endpoint;

/**
 * Publishes the APU source management service on the CXF bus and servlet
 * registered by {@code cz.aron.ft.server.CxfWsConfig}: {@code /cxf/management},
 * next to the file-transfer endpoint {@code /cxf/ft}. Both are internal
 * service-to-service interfaces for Transfagent.
 * <p>
 * The committed WSDL is the contract shared with the caller, so it is served
 * as-is under {@code ?wsdl} rather than generated from the annotated interface.
 */
@Configuration
public class ApuManagementWsConfig {

	@Bean
	public Endpoint endpointApuManagement(Bus bus, ApuManagementEndpoint apuManagementEndpoint) {
		EndpointImpl endpoint = new EndpointImpl(bus, apuManagementEndpoint);
		endpoint.setServiceName(AronManagementService.SERVICE);
		endpoint.setEndpointName(AronManagementService.ApuManagementPort);
		endpoint.setWsdlLocation(ApuManagementWsConfig.class.getResource("/wsdl/aron_core.wsdl").toExternalForm());
		endpoint.publish("/management");
		return endpoint;
	}

}
