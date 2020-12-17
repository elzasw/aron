package cz.aron.transfagent.service;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServiceConfig {

	public final static String TRANSFORM_AGENT_MANAGER_URL = "/ApuTransformManager";
	
	private final ApuTransformManagerService apuTransformManagerService;
	    
		
	public WebServiceConfig(final ApuTransformManagerService apuTransformManagerService) {
		this.apuTransformManagerService = apuTransformManagerService;
	}
	
    @Bean(name = Bus.DEFAULT_BUS_ID)
    public SpringBus springBus() {
        return new SpringBus();
    }

	
    @Bean
    public ServletRegistrationBean<CXFServlet> cxfServlet() {
        return new ServletRegistrationBean<CXFServlet>(new CXFServlet(), "/ws/*");
    }

	
    @Bean
    public Endpoint apuTransformManagerServiceEndpoint() {
        EndpointImpl endpoint = new EndpointImpl(springBus(), apuTransformManagerService);
        String wsdlLocation = WebServiceConfig.class.getResource("/wsdl/aron_transform_agent.wsdl")
                .toExternalForm();
        endpoint.setWsdlLocation(wsdlLocation);
        endpoint.publish(TRANSFORM_AGENT_MANAGER_URL);
        return endpoint;
    }
	
}
