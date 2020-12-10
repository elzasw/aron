package cz.aron.transfagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.ClientConfig;
import com.lightcomp.ft.client.UploadRequest;
import com.lightcomp.ft.simple.UploadRequestImpl;
import com.lightcomp.ft.xsd.v1.GenericDataType;

import cz.aron.transfagent.config.ConfigAronCore;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.repository.CoreQueueRepository;

@Service
public class CoreQueueService implements SmartLifecycle {

    static final Logger log = LoggerFactory.getLogger(CoreQueueService.class);

    @Autowired
    CoreQueueRepository coreQueueRepository;

    @Autowired
    ConfigAronCore configAronCore;

    private ThreadStatus status;

    /**
     * Odeslání dat do jádra
     */
    private void sendData() {
        if (coreQueueRepository.count() > 0) {
            CoreQueue item = coreQueueRepository.findFirstByOrderById();
            uploadData(item);
            coreQueueRepository.delete(item);
        }
    }

    /**
     * Přenos dat pomocí FileTransfer
     * 
     * @param item
     */
    private void uploadData(CoreQueue item) {

        // vytváření Clienta
        ClientConfig clientConfig = new ClientConfig(configAronCore.getUrl());
        clientConfig.setSoapLogging(configAronCore.getSoapLogging());

        Client client = FileTransfer.createClient(clientConfig);

        // vytváření Requesta
        GenericDataType dataType = new GenericDataType();
        UploadRequest request = new UploadRequestImpl(clientConfig.getWorkDir(), dataType);
        try {
            client.uploadSync(request);
        } finally {
            client.stop();
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
