package cz.aron.core.ws;

import cz.aron.apux._2020.UuidList;
import cz.aron.core.model.ApuRepository;
import cz.aron.core.model.ApuSourceStore;
import cz.aron.core.model.ApuStore;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 05.11.2020.
 */
@Endpoint
public class AronCoreEndpoint {
    private static final String NAMESPACE_URI = "http://www.aron.cz/management/v1";

    @Inject private ApuRepository apuRepository;
    @Inject private ApuSourceStore apuSourceStore;
    @Inject private ApuStore apuStore;

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "deleteApuSourcesRequest")
    @ResponsePayload
    @Transactional
    public void deleteApuSources(@RequestPayload UuidList request) {
        for (String sourceId : request.getUuid()) {
            List<String> apusToDelete = apuStore.findBySourceId(sourceId);
            apuRepository.delete(apusToDelete);
            apuSourceStore.delete(sourceId);
        }
    }
}
