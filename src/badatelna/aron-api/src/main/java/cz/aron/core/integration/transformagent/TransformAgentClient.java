package cz.aron.core.integration.transformagent;

import cz.aron.apux._2020.UuidList;
import cz.aron.transform_agent.v1.ObjectFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.addressing.client.ActionCallback;

import java.net.URISyntaxException;

/**
 * @author Lukas Jane (inQool) 06.11.2020.
 */
@Slf4j
public class TransformAgentClient extends WebServiceGatewaySupport {
    public void getCountry(String country) {
        ObjectFactory objectFactory = new ObjectFactory();
        objectFactory.createRequestApus(new UuidList());

        ActionCallback actionCallback;
        try {
            actionCallback = new ActionCallback("www.aron.cz/transformer/request-apus");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Object whateverCame = getWebServiceTemplate()
                .marshalSendAndReceive(objectFactory, actionCallback);
        log.info("we made it to end");
    }
}
