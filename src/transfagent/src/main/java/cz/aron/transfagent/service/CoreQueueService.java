package cz.aron.transfagent.service;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import com.lightcomp.ft.FileTransfer;
import com.lightcomp.ft.client.Client;
import com.lightcomp.ft.client.ClientConfig;
import com.lightcomp.ft.core.send.items.ListReader;
import com.lightcomp.ft.core.send.items.SimpleFile;
import com.lightcomp.ft.core.send.items.SourceItem;

import cz.aron.transfagent.config.ConfigAronCore;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.service.client.CoreAronClient;
import cz.aron.apux._2020.UuidList;
import cz.aron.management.v1.ApuManagementPort;

@Service
public class CoreQueueService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(CoreQueueService.class);

    private final CoreQueueRepository coreQueueRepository;

    private final ConfigAronCore configAronCore;
    
    private final CoreAronClient coreAronClient;

    private final StorageService storageService;

    private ThreadStatus status;

    public CoreQueueService(CoreQueueRepository coreQueueRepository, ConfigAronCore configAronCore, CoreAronClient coreAronClient, StorageService storageService) {
        this.coreQueueRepository = coreQueueRepository;
        this.configAronCore = configAronCore;
        this.coreAronClient = coreAronClient; 
        this.storageService = storageService;
    }

    /**
     * Odeslání dat do jádra
     */
    private void sendData() {
        if (coreQueueRepository.count() > 0) {
            CoreQueue item = coreQueueRepository.findFirstByOrderById();
            uploadOrDeleteData(item);
            coreQueueRepository.delete(item);
        }
    }

    /**
     * Přenos dat pomocí FileTransfer nebo mazání přes WSDL dotaz
     * 
     * @param item
     */
    private void uploadOrDeleteData(CoreQueue item) {
        if (item.getApuSource().isDeleted()) { // dotaz na vymazání záznamu
            deleteItemData(item);
        } else {
            uploadItemData(item);
        }
    }

    /**
     * Mazání dat přes WSDL dotaz
     * 
     * @param item
     */
    private void deleteItemData(CoreQueue item) {
        UuidList uuidList = new UuidList();
        uuidList.getUuid().add(item.getApuSource().getUuid().toString());
        ApuManagementPort apuManagementPort = coreAronClient.get();
        try {
            apuManagementPort.deleteApuSources(uuidList);
        } catch (Exception e) {
            item.setErrorMessage(e.getMessage());
            coreQueueRepository.save(item);
            log.error("Error deleting apusrc.", e);
            throw new IllegalStateException(e);
        }
        log.info("Deleted apusrc={}, type={}", item.getApuSource().getUuid(), item.getApuSource().getSourceType());
    }

    /**
     * Přenos dat přes FileTransfer
     * 
     * @param item
     */
    private void uploadItemData(CoreQueue item) {
        // vytváření Clienta
        ClientConfig clientConfig = new ClientConfig(configAronCore.getFt().getUrl());
        clientConfig.setSoapLogging(configAronCore.getFt().getSoapLogging());
        Client client = FileTransfer.createClient(clientConfig);

        List<SourceItem> sourceItems = createSourceItems(item);
        UploadRequestImpl request = UploadRequestImpl.buildRequest(new ListReader(sourceItems),""+item.getId());
        try {
            client.uploadSync(request);
        } finally {
            client.stop();
        }

        String errorMsg = null;
        if (request.isCanceled()) {
            errorMsg = "Transfer to server canceled.";  
        }
        if (request.isFailed()) {
            errorMsg = "Transfer to server failed.";  
        }

        if (!StringUtils.isBlank(errorMsg)) {
            item.setErrorMessage(errorMsg);
            coreQueueRepository.save(item);
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        log.info("Uploaded apusrc={}, type={}", item.getApuSource().getUuid(), item.getApuSource().getSourceType());
    }

    private List<SourceItem> createSourceItems(CoreQueue item) {
    	switch (item.getApuSource().getSourceType()) {
    	case DIRECT:
    		return createSourceItemsDirect(item);
    	case INSTITUTION:
    		return createSourceItemsInstitution(item);
    	default:
    		throw new IllegalStateException();
    	}
    }

    private List<SourceItem> createSourceItemsDirect(CoreQueue item) {
        Path dataDir = storageService.getApuDataDir(item.getApuSource().getDataDir());
        File [] files = dataDir.toFile().listFiles((f)->f.isFile());

        Path file;
        if (files!=null&&files.length==1) {
        	file = files[0].toPath();
        } else {
        	throw new IllegalStateException();
        }

        List<SourceItem> sourceItems = new ArrayList<>();        
        SimpleFile simpleFile = new SimpleFile(file, "apusrc-"+item.getApuSource().getUuid()+".xml");
        sourceItems.add(simpleFile);
        return sourceItems;
    }

    private List<SourceItem> createSourceItemsInstitution(CoreQueue item) {
    	Path dataDir = storageService.getApuDataDir(item.getApuSource().getDataDir());
    	Path file = dataDir.resolve("apusrc.xml");
        List<SourceItem> sourceItems = new ArrayList<>();
        SimpleFile simpleFile = new SimpleFile(file, "apusrc-"+item.getApuSource().getUuid()+".xml");
        sourceItems.add(simpleFile);
        return sourceItems;
    }

    public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                sendData();
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("Error in sending data. ", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    return;
                }
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
