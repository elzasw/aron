package cz.aron.core.ft.server;

import com.lightcomp.ft.wsdl.v1.FileTransferService_Service;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;
import javax.xml.ws.Endpoint;

@Configuration
public class CxfWsConfig {
    @Bean
    public ServletRegistrationBean<CXFServlet> cxfServlet() {
        ServletRegistrationBean<CXFServlet> cxfServletServletRegistrationBean = new ServletRegistrationBean<>(new CXFServlet(), "/cxf/*");
        MultipartConfigElement multipartConfigElement = new MultipartConfigElement("");
        cxfServletServletRegistrationBean.setMultipartConfig(multipartConfigElement);
        return cxfServletServletRegistrationBean;
    }

    @Bean(name = Bus.DEFAULT_BUS_ID)
    public SpringBus springBus() {
        return new SpringBus();
    }

    @Bean
    public Endpoint endpointFileTransfer(FileTransferServer fileTransferServer) {
        EndpointImpl endpoint = fileTransferServer.getServer().getEndpointFactory().createCxf(springBus());
        endpoint.setServiceName(FileTransferService_Service.SERVICE);
        String wsdlLocation = CxfWsConfig.class.getResource("/wsdl/file-transfer-v1.wsdl").toExternalForm();
        endpoint.setWsdlLocation(wsdlLocation);
        endpoint.publish("/ft");
        return endpoint;
    }
}
