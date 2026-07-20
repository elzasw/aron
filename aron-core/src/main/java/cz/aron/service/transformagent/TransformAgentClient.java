package cz.aron.service.transformagent;

import cz.aron.apux._2020.UuidList;
import cz.aron.transform_agent.v1.ObjectFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import java.util.List;

public class TransformAgentClient extends WebServiceGatewaySupport {
	
	private static final Logger log = LoggerFactory.getLogger(TransformAgentClient.class);
	
    public void requestApus(List<String> apuIds) {
        ObjectFactory objectFactory = new ObjectFactory();
        UuidList uuidList = new UuidList();
        uuidList.getUuid().addAll(apuIds);
        Object whateverCame = getWebServiceTemplate()
                .marshalSendAndReceive(objectFactory.createRequestApus(uuidList));  //they crash on action
        log.info("{} apus requested from transform agent", apuIds.size());
    }
}
