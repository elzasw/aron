package cz.aron.transfagent.par;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cz.aron.peva2.wsdl.PEvAService;
import cz.aron.transfagent.config.ConfigPar;
import cz.nacr.nda.par.api._2019.ArchivyPort;

@Configuration
public class ParClient {

	@Bean
	public ArchivyPort pr(ConfigPar config) {
		JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
		factoryBean.setServiceClass(PEvAService.class);
		factoryBean.setAddress(config.getUrl());
		if (config.getUsername() != null) {
			factoryBean.setUsername(config.getUsername());
			factoryBean.setPassword(config.getPassword());
		}
		if (config.isSoapLogging()) {
			LoggingFeature lf = new LoggingFeature();
			lf.setPrettyLogging(true);
			factoryBean.getFeatures().add(lf);
		}

		ArchivyPort archivyPort = factoryBean.create(ArchivyPort.class);

		// set timeout by default on 5 minutes
		Client client = ClientProxy.getClient(archivyPort);
		HTTPConduit http = (HTTPConduit) client.getConduit();
		http.getClient().setConnectionTimeout(5 * 60000);
		http.getClient().setReceiveTimeout(5 * 60000);

		// wssecurity
		/*
		if (config.getUsername() != null) {
			Map<String, Object> outProps = new HashMap<String, Object>();
			outProps.put(WSHandlerConstants.ACTION,
					WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.TIMESTAMP);
			outProps.put(WSHandlerConstants.USER, config.getUsername());
			// Password type : plain text
			outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
			outProps.put(WSHandlerConstants.PW_CALLBACK_REF, new CallbackHandler() {
				public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
					WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
					if (pc.getIdentifier().equals(config.getUsername())) {
						pc.setPassword(config.getPassword());
					}
				}
			});
			WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
			client.getEndpoint().getOutInterceptors().add(wssOut);
		}*/
		return archivyPort; 
	}

}
