package cz.aron.transfagent.service;

import javax.annotation.PostConstruct;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cz.tacr.elza.ws.core.v1.ExportService;


@Service
public class ElzaExportService {
	
    @Value("${elza.url}")
    private String elzaUrl;

    @Value("${elza.username}")
    private String elzaUsername;

    @Value("${elza.password}")
    private String elzaPassword;

    @Value("${elza.soapLogging}")
    private boolean soapLogging;

    private ExportService exportService;

    @PostConstruct
    public void init() {  	
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setServiceClass(ExportService.class);
        factoryBean.setAddress(elzaUrl);
        factoryBean.setUsername(elzaUsername);
        factoryBean.setPassword(elzaPassword);
        if (soapLogging) {
            LoggingFeature lf = new LoggingFeature();
            lf.setPrettyLogging(true);
            factoryBean.getFeatures().add(lf);
        }

        exportService = factoryBean.create(ExportService.class);
        
        // set timeout by default on 5 minutes
        Client client = ClientProxy.getClient(exportService);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.getClient().setConnectionTimeout(5*60000);
        http.getClient().setReceiveTimeout(5*60000);
    }

    public ExportService get() {
        return exportService;
    }

}
