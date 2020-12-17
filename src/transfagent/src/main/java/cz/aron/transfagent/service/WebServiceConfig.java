package cz.aron.transfagent.service;

import javax.xml.ws.Endpoint;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource({"classpath:META-INF/cxf/cxf.xml", "classpath:META-INF/cxf/cxf-servlet.xml"})
public class WebServiceConfig {

	public final static String TRANSFORM_AGENT_MANAGER_URL = "/ApuTransformManager";
	
	private final ApuTransformManagerService apuTransformManagerService;
	    
    private final SpringBus bus;	
	
	public WebServiceConfig(final ApuTransformManagerService apuTransformManagerService,
							final SpringBus bus) {
		this.apuTransformManagerService = apuTransformManagerService;
		this.bus = bus;
	}
	
    @Bean
    public Endpoint daoDigitizationServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(bus, apuTransformManagerService);
        endpoint.publish(TRANSFORM_AGENT_MANAGER_URL);
        return endpoint;
    }
	
}
