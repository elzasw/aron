package cz.aron.transfagent.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigAronFileImport;
import cz.aron.transfagent.service.importfromdir.FileHelper;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

@Service
public class FileImportService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FileImportService.class);        

    private final StorageService storageService;

    private final ConfigAronFileImport configAronFileImport;

    List<ImportProcessor> importProcessors = new ArrayList<>();

    private long importInterval = 5000;

    private ThreadStatus status;

    public FileImportService(StorageService storageService, ConfigAronFileImport configAronFileImport) throws IOException {
        this.storageService = storageService;
        this.configAronFileImport = configAronFileImport;
        initDirs();
    }

    /**
     * Inicializace pracovních adresářů
     * 
     * @throws IOException
     */
    private void initDirs() throws IOException {
    	FileHelper.createDirIfNotExists(storageService.getDataPath());
    	FileHelper.createDirIfNotExists(storageService.getErrorPath());
    	FileHelper.createDirIfNotExists(storageService.getDaoPath());
    }

    /**
     * Monitorování vstupního adresáře
     * @param ic 
     * 
     * @throws IOException
     */
    private void importData(ImportContext ic) throws IOException {    	    	
    	for(var importProcessor: importProcessors) {
    	    log.debug("Starting import, processor: {}", importProcessor.getClass().getName());
    		importProcessor.importData(ic);
    		log.debug("Final processor state: failed={}, processedItems={}", ic.isFailed(), ic.getNumProcessed());
            if(ic.isFailed()||ic.getNumProcessed()>0) {                
            	return;
            }
    	}
    }
    
    private void logProcessors() {
    	for(var importProcessor: importProcessors) {
    		log.info("Import processor={}, priority={}",importProcessor.getClass(),importProcessor.getPriority());
    	}
    }

    public void run() {
        log.info("Import service is running.");
        logProcessors();
        
        while (status == ThreadStatus.RUNNING) {
            try {
            	ImportContext ic = new ImportContext();
                importData(ic);
                if(ic.isFailed()||ic.getNumProcessed()==0) {
                	Thread.sleep(importInterval);
                }
            } catch (Exception e) {
                log.error("Error in import file. ", e);
                try {
					Thread.sleep(importInterval);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
					return;
				}
            }
        }
        log.info("Import service stopped.");
        status = ThreadStatus.STOPPED;
    }

    @Override
    public void start() {
        if(configAronFileImport.isFileImportDisabled()) {
            status = ThreadStatus.STOPPED;
            return;
        }
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

	public void registerImportProcessor(ImportProcessor importProcessor) {
	    for(int i=0; i<importProcessors.size(); i++) {
	        var p = importProcessors.get(i);
	        if(importProcessor.getPriority()>p.getPriority()) {
	            importProcessors.add(i, importProcessor);
	            return;
	        }		
	    }
	    importProcessors.add(importProcessor);
	}

}
