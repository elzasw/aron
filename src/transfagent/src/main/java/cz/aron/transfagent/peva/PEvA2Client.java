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
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import cz.aron.peva2.wsdl.PEvA;
import cz.aron.peva2.wsdl.PEvAService;
import cz.aron.peva2.wsdl.RoleLevel;
import cz.aron.peva2.wsdl.RoleName;
import cz.aron.peva2.wsdl.UserDetails;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigPeva2InstitutionCredentials;

@Configuration
public class PEvA2Client {

    /**
     * Defaultni pripojeni na pevu
     * 
     * @param config
     * @return PEvA
     */
    @Bean
    @Primary
    public PEvA2Connection peva2Main(ConfigPeva2 config) {
        PEvA peva2 = peva2(config.getUrl(), config.getUsername(), config.getPassword(), config.isSoapLogging());
        fillHeaders(peva2, createUserDetails(config.getRole(),config.getInstitutionId(),config.getUserId()));
        return new PEvA2Connection(peva2,config.getInstitutionId(),config.getUsername(),config.getUserId(),true);
    }

    
    public static final String PEVA2_INST_BEAN_NAME = "peva2Inst";
    
    /**
     * Pripojeni pro podrizene archivy
     * @param config
     * @param institutionCredentials
     * @return PEvA
     */
    @Bean
    @Scope("prototype")
    public PEvA2Connection peva2Inst(ConfigPeva2 config, ConfigPeva2InstitutionCredentials institutionCredentials) {
        PEvA peva2 = peva2(config.getUrl(), institutionCredentials.getUsername(), institutionCredentials
                .getPassword() != null ? institutionCredentials.getPassword() : config.getPassword(), config
                        .isSoapLogging());
        fillHeaders(peva2, createUserDetails(config.getRole(), institutionCredentials.getInstitutionId(),config.getUserId()));
        return new PEvA2Connection(peva2, institutionCredentials.getInstitutionId(), institutionCredentials
                .getUsername(), config.getUserId(), false);
    }

    private PEvA peva2(String url, String username, String password, boolean soapLogging) {
        JaxWsProxyFactoryBean factoryBean = new JaxWsProxyFactoryBean();
        factoryBean.setServiceClass(PEvAService.class);
        factoryBean.setAddress(url);
        factoryBean.setUsername(username);
        factoryBean.setPassword(password);
        if (soapLogging) {
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
        outProps.put(WSHandlerConstants.USER, username);
        // Password type : plain text
        outProps.put(WSHandlerConstants.PASSWORD_TYPE, WSConstants.PW_TEXT);
        outProps.put(WSHandlerConstants.PW_CALLBACK_REF, new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
                if (pc.getIdentifier().equals(username)) {
                    pc.setPassword(password);
                }
            }
        });
        WSS4JOutInterceptor wssOut = new WSS4JOutInterceptor(outProps);
        client.getEndpoint().getOutInterceptors().add(wssOut);
        return peva;
    }
    
    private UserDetails createUserDetails(String role, String institutionId, String userId) {
        UserDetails ud = new UserDetails();
        ud.setRoleName(RoleName.VIEWER);
        RoleLevel roleLevel = RoleLevel.BASIC;
        if (role != null) {
            roleLevel = RoleLevel.fromValue(role);
        }
        ud.setRoleLevel(roleLevel);
        ud.setInstitutionId(institutionId);
        ud.setUserId(userId);
        return ud;
    }

    private void fillHeaders(PEvA peva2, UserDetails userDetails) {
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
