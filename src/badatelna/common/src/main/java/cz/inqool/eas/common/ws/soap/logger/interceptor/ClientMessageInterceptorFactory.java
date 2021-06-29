package cz.inqool.eas.common.ws.soap.logger.interceptor;

import cz.inqool.eas.common.ws.soap.logger.message.SoapMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

public class ClientMessageInterceptorFactory {
    private SoapMessageRepository soapMessageRepository;

    private PlatformTransactionManager transactionManager;
    
    public ClientMessageInterceptor createInstance(String serviceName) {
        return new ClientMessageInterceptor(serviceName, soapMessageRepository, transactionManager);
    }

    @Autowired
    public void setSoapMessageRepository(SoapMessageRepository soapMessageRepository) {
        this.soapMessageRepository = soapMessageRepository;
    }

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
