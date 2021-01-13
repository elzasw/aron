package cz.aron.core.integration;

import cz.aron.core.integration.transformagent.TransformAgentClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 17.12.2020.
 */
@Service
@Slf4j
public class ApuRequestQueue {

    @Inject private QueuedApuStore queuedApuStore;
    @Inject private TransformAgentClient transformAgentClient;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Async
    public void add(String apuId, String sourceId) {
        QueuedApu queuedApu = new QueuedApu();
        queuedApu.setApuId(apuId);
        queuedApu.setSourceApuId(sourceId);
        queuedApuStore.create(queuedApu);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Async
    public void sendRequests() {
        try {
            List<QueuedApu> batchToResolve = queuedApuStore.getBatchToResolve();
            if (batchToResolve.isEmpty()) {
                return;
            }
            List<String> requestedIdsList = new ArrayList<>();
            for (QueuedApu apu : batchToResolve) {
                requestedIdsList.add(apu.getApuId());
                apu.setRequestSent(true);
            }
            transformAgentClient.requestApus(requestedIdsList);
            queuedApuStore.update(batchToResolve);
        }
        catch (Exception e) {
            log.error("Failed to send related APU requests.", e);
        }
    }

    public void removeForApuId(String apuId) {
        queuedApuStore.removeForApuId(apuId);
    }
}
