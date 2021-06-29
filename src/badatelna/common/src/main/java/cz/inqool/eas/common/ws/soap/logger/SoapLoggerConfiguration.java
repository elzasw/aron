package cz.inqool.eas.common.ws.soap.logger;

import cz.inqool.eas.common.ws.soap.logger.interceptor.ClientMessageInterceptorFactory;
import cz.inqool.eas.common.ws.soap.logger.message.SoapMessageApi;
import cz.inqool.eas.common.ws.soap.logger.message.SoapMessageRepository;
import cz.inqool.eas.common.ws.soap.logger.message.SoapMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for SOAP logger subsystem.
 *
 * If application wants to use SOAP logger subsystem,
 * it needs to extend this class and add {@link Configuration} annotation.
 *
 */
@Slf4j
public abstract class SoapLoggerConfiguration {
    @Autowired
    private ConfigurableEnvironment env;


    /**
     * Adds custom property source to spring for setting the url path of {@link SoapMessageApi}.
     */
    @PostConstruct
    public void registerPropertySource() {
        log.debug("Generating property source for soap_logger_configuration");

        Map<String, Object> properties = new HashMap<>();
        properties.put("soap-logger.message.url", soapLoggerMessageUrl());

        MapPropertySource propertySource = new MapPropertySource("soap_logger_configuration", properties);
        env.getPropertySources().addLast(propertySource);
    }

    /**
     * Constructs {@link SoapMessageRepository} bean.
     */
    @Bean
    public SoapMessageRepository soapMessageRepository() {
        return new SoapMessageRepository();
    }

    /**
     * Constructs {@link SoapMessageService} bean.
     */
    @Bean
    public SoapMessageService soapMessageService() {
        return new SoapMessageService();
    }

    /**
     * Constructs {@link SoapMessageApi} bean.
     */
    @Bean
    public SoapMessageApi soapMessageApi() {
        return new SoapMessageApi();
    }

    @Bean
    public ClientMessageInterceptorFactory clientMessageInterceptorFactory() {
        return new ClientMessageInterceptorFactory();
    }

    /**
     * Returns url path of {@link SoapMessageApi}.
     */
    protected abstract String soapLoggerMessageUrl();
}
