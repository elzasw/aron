package cz.aron.transfagent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.aron.apux._2020.UuidList;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transform_agent.v1.ApuTransformManager;

@Service
public class ApuTransformManagerService implements ApuTransformManager {

    private static final Logger log = LoggerFactory.getLogger(ApuTransformManagerService.class);

    @Autowired
    ArchivalEntityRepository archivalEntityRepository;

    @Autowired
    CoreQueueRepository coreQueueRepository;

    @Override
    public void requestApus(UuidList requestApus) {
    	log.debug("Received WSDL Apu request");
    	
        Validate.notNull(requestApus, "The request must not be null");

        List<UUID> uuids = requestApus.getUuid().stream()
                .map(p -> UUID.fromString(p))
                .collect(Collectors.toList());

        if (!uuids.isEmpty()) {
            List<ArchivalEntity> archivalEntities = archivalEntityRepository.findByUuidInAndStatus(uuids,
                                                                                                   EntityStatus.AVAILABLE);
            if (!archivalEntities.isEmpty()) {
                List<CoreQueue> coreQueues = new ArrayList<>(archivalEntities.size());
                for (ArchivalEntity entita : archivalEntities) {
                    CoreQueue coreQueue = new CoreQueue();
                    coreQueue.setApuSource(entita.getApuSource());
                }

                coreQueueRepository.saveAll(coreQueues);
                log.info("Sent to CoreQueue {} record(s).", coreQueues.size());
            }
        }
        log.info("Processed WSDL Apu request");
    }

}
