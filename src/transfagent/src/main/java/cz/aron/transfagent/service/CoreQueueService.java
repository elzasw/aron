package cz.aron.transfagent.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.ClientConfig;
import com.lightcomp.ft.client.UploadRequest;
import com.lightcomp.ft.core.send.items.ListReader;
import com.lightcomp.ft.core.send.items.SimpleFile;
import com.lightcomp.ft.core.send.items.SourceItem;
import com.lightcomp.ft.xsd.v1.GenericDataType;

import cz.aron.transfagent.config.ConfigAronCore;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.repository.CoreQueueRepository;

@Service
public class CoreQueueService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(CoreQueueService.class);

    private final CoreQueueRepository coreQueueRepository;

    private final ConfigAronCore configAronCore;
    
    private final StorageService storageService;

    private ThreadStatus status;

    public CoreQueueService(CoreQueueRepository coreQueueRepository, ConfigAronCore configAronCore, StorageService storageService) {
    	this.coreQueueRepository = coreQueueRepository;
    	this.configAronCore = configAronCore;
    	this.storageService = storageService;
    }
    
    /**
     * Odeslání dat do jádra
     */
    private void sendData() {
        if (coreQueueRepository.count() > 0) {
            CoreQueue item = coreQueueRepository.findFirstByOrderById();
            uploadData(item);            
            try {
				storageService.moveToProcessed(storageService.getApuDataDir(item.getApuSource().getDataDir()));
			} catch (IOException e) {
				log.error("Fail to move to processed directory {}",item.getApuSource().getDataDir(),e);
			}            
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
        
        Path dataDir = storageService.getApuDataDir(item.getApuSource().getDataDir());
        File [] files = dataDir.toFile().listFiles((f)->f.isFile());
        
        Path file;
        if (files!=null&&files.length==1) {
        	file = files[0].toPath();
        } else {
        	throw new IllegalStateException();
        }
        
        List<SourceItem> sourceItems = new ArrayList<>();
        SimpleFile simpleFile = new SimpleFile(file,"apusrc-"+item.getApuSource().getUuid()+".xml");
        sourceItems.add(simpleFile);
        
        UploadRequestImpl request = UploadRequestImpl.buildRequest(new ListReader(sourceItems),""+item.getApuSource().getId());
        try {
            client.uploadSync(request);
        } finally {
            client.stop();
        }

        if (request.isCanceled()) {
            log.error("transfer to server canceled.");
            throw new IllegalStateException();
        }
        if (request.isFailed()) {
            log.error("transfer to server failed.");
            throw new IllegalStateException();
        }
        
    }

    public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                sendData();
                Thread.sleep(5000);
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
