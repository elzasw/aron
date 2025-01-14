package cz.inqool.eas.common.client.export.runner;

import cz.inqool.eas.common.client.ClientRequestBuilder;
import cz.inqool.eas.common.client.export.ExportClientException;
import cz.inqool.eas.common.export.request.ExportRequest;
import cz.inqool.eas.common.security.service.ServiceHub;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.session.Session;

/**
 * Client for export runner subsystem using inter-service communication.
 */
@Slf4j
public class ExportRunnerClient {
    private Session clientSession;

    private ServiceHub<? extends Session> serviceHub;

    private final String serviceUrl;

    private final String path;

    @Builder
    public ExportRunnerClient(String serviceUrl, String path) {
        this.serviceUrl = serviceUrl;
        this.path = path;
    }

    public void process(String id) {
        try {
            serviceHub.doInSession(clientSession, (template) -> {
                ClientRequestBuilder.<ExportRequest>post()
                        .urlPath(path + "/process/" + id)
                        .headers()
                        .setContentType(MediaType.APPLICATION_JSON).set()
                        .execute(template, serviceUrl);
            });
        } catch (Exception exception) {
            log.error("Failed to acquire export request.", exception);
            throw new ExportClientException("Failed to acquire export request.", exception);
        }
    }

    @Autowired
    public void setClientSession(Session clientSession) {
        this.clientSession = clientSession;
    }

    @Autowired
    public void setServiceHub(ServiceHub<? extends Session> serviceHub) {
        this.serviceHub = serviceHub;
    }
}
