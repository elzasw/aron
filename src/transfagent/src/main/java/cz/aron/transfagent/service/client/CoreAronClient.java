package cz.aron.transfagent.service.client;

import javax.annotation.PostConstruct;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.aron.management.v1.ApuManagementPort;
import cz.aron.transfagent.config.ConfigAronCore;

@Component
public class CoreAronClient {

	@Autowired
	ConfigAronCore configAronCore;

	private ApuManagementPort apuManagementPort;

    @PostConstruct
    public void init() {  	
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setServiceClass(ApuManagementPort.class);
        factoryBean.setAddress(configAronCore.getCore().getUrl());
        factoryBean.setUsername(configAronCore.getCore().getUser());
        factoryBean.setPassword(configAronCore.getCore().getPass());
        if (configAronCore.getCore().getSoapLogging()) {
            LoggingFeature lf = new LoggingFeature();
            lf.setPrettyLogging(true);
            factoryBean.getFeatures().add(lf);
        }

        apuManagementPort = factoryBean.create(ApuManagementPort.class);

        // set timeout by default on 5 minutes
        Client client = ClientProxy.getClient(apuManagementPort);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.getClient().setConnectionTimeout(5*60000);
        http.getClient().setReceiveTimeout(5*60000);
    }

    public ApuManagementPort get() {
        return apuManagementPort;
    }

}
