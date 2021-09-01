package cz.aron.transfagent.peva;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.bind.JAXBException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import cz.aron.peva2.wsdl.PEvA;
import cz.aron.peva2.wsdl.PEvAService;
import cz.aron.peva2.wsdl.RoleLevel;
import cz.aron.peva2.wsdl.RoleName;
import cz.aron.peva2.wsdl.UserDetails;
import cz.aron.transfagent.config.ConfigPeva2;

@Configuration
public class PEvA2Client {
 
    @Bean
    public PEvA peva2(ConfigPeva2 config) {
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setServiceClass(PEvAService.class);
        factoryBean.setAddress(config.getUrl());
        factoryBean.setUsername(config.getUsername());
        factoryBean.setPassword(config.getPassword());
        if (config.isSoapLogging()) {
            LoggingFeature lf = new LoggingFeature();
            lf.setPrettyLogging(true);
            factoryBean.getFeatures().add(lf);
        }

        PEvA peva = factoryBean.create(PEvA.class);

        // set timeout by default on 5 minutes
        Client client = ClientProxy.getClient(peva);
        HTTPConduit http = (HTTPConduit) client.getConduit();
        http.getClient().setConnectionTimeout(5 * 60000);
        http.getClient().setReceiveTimeout(5 * 60000);

        // wssecurity
        Map<String, Object> outProps = new HashMap<String, Object>();
        outProps.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN + " " + WSHandlerConstants.TIMESTAMP);
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
        return peva;
    }

    
    public static UserDetails createUserDetails(ConfigPeva2 config) {
        UserDetails ud = new UserDetails();
        ud.setRoleName(RoleName.VIEWER);
        RoleLevel roleLevel = RoleLevel.BASIC;
        if (config.getRole()!=null) {
            roleLevel = RoleLevel.fromValue(config.getRole());
        }
        ud.setRoleLevel(roleLevel);
        ud.setInstitutionId(config.getInstitutionId());
        ud.setUserId(config.getUserId());
        return ud;
    }
    
    public static void fillHeaders(PEvA peva2, ConfigPeva2 config) {
        UserDetails userDetails = createUserDetails(config);
        List<Header> headers = new ArrayList<Header>();
        Header soapHeader;
        try {
            soapHeader = new Header(Peva2XmlReader.OBJECT_FACTORY.createUserDetails(userDetails).getName(), userDetails,
                    new JAXBDataBinding(UserDetails.class));
        } catch (JAXBException e) {
            throw new IllegalStateException();
        }
        headers.add(soapHeader);
        org.apache.cxf.endpoint.Client p = org.apache.cxf.frontend.ClientProxy.getClient(peva2);
        Map<String, Object> ctx = p.getRequestContext();
        ctx.put(Header.HEADER_LIST, headers);
    }


}
