package cz.aron.transfagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.repository.CoreQueueRepository;

@Service
public class CoreQueueService implements SmartLifecycle {

    static final Logger log = LoggerFactory.getLogger(CoreQueueService.class);

    @Autowired
    CoreQueueRepository coreQueueRepository;

    private ThreadStatus status;

    /**
     * Odeslání pomocí WSDL data do jádra
     */
    private void sendData() {
        if (coreQueueRepository.count() > 0) {
            CoreQueue item = coreQueueRepository.findFirstByOrderById();

            // TODO send to core

            coreQueueRepository.delete(item);
        }
    }

    public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                sendData();
            } catch (Exception e) {
                log.error("Error in sending data. ", e);
            }
        }
        status = ThreadStatus.STOPPED;
    }

    @Override
    public void start() {
        status = ThreadStatus.RUNNING;
        new Thread(() -> {
            run();
        }).start();
    }

    @Override
    public void stop() {
        status = ThreadStatus.STOP_REQUEST;
    }

    @Override
    public boolean isRunning() {
        return status == ThreadStatus.RUNNING;
    }
}
