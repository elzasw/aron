package cz.aron.core.integration.transformagent;

import cz.aron.apux._2020.UuidList;
import cz.aron.transform_agent.v1.ObjectFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import java.util.List;

/**
 * @author Lukas Jane (inQool) 06.11.2020.
 */
@Slf4j
public class TransformAgentClient extends WebServiceGatewaySupport {
    public void requestApus(List<String> apuIds) {
        ObjectFactory objectFactory = new ObjectFactory();
        UuidList uuidList = new UuidList();
        uuidList.getUuid().addAll(apuIds);
        Object whateverCame = getWebServiceTemplate()
                .marshalSendAndReceive(objectFactory.createRequestApus(uuidList));  //they crash on action
        log.info("{} apus requested from transform agent", apuIds.size());
    }
}
