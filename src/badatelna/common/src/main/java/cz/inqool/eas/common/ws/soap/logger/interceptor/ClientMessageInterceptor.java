package cz.inqool.eas.common.ws.soap.logger.interceptor;

import com.google.common.base.Charsets;
import cz.inqool.eas.common.ws.soap.logger.message.SoapMessage;
import cz.inqool.eas.common.ws.soap.logger.message.SoapMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptorAdapter;
import org.springframework.ws.context.MessageContext;

import java.io.ByteArrayOutputStream;

/**
 * Interceptor for logging request/response records of web service client
 */
@Slf4j
public class ClientMessageInterceptor extends ClientInterceptorAdapter {

    private final SoapMessageRepository messageRepository;

    private final PlatformTransactionManager transactionManager;

    private final String serviceName;

    public ClientMessageInterceptor(String serviceName, SoapMessageRepository messageRepository, PlatformTransactionManager transactionManager) {
        this.serviceName = serviceName;
        this.messageRepository = messageRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public void afterCompletion(MessageContext context, Exception ex) throws WebServiceClientException {
        WebServiceMessage request = context.getRequest();
        WebServiceMessage response = context.getResponse();

        SoapMessage message = new SoapMessage();
        message.setService(serviceName);
        message.setRequest(formatMessage(request));
        message.setResponse(formatMessage(response));

        // fixme find out why @TransactionalNew annotation inside isn't applied
        // fixme: most likely already fixed by next call
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.execute(status -> messageRepository.create(message));
    }

    private String formatMessage(WebServiceMessage message) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            message.writeTo(stream);
            return stream.toString(Charsets.UTF_8.name());
        } catch (Exception ex) {
            log.error("Failed to log SOAP message.", ex);
            return "error";
        }
    }
}

