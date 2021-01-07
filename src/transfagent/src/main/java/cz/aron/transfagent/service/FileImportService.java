package cz.aron.transfagent.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.service.importfromdir.FileHelper;
import cz.aron.transfagent.service.importfromdir.ImportArchDescService;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportDaoService;
import cz.aron.transfagent.service.importfromdir.ImportDirectService;
import cz.aron.transfagent.service.importfromdir.ImportFundService;
import cz.aron.transfagent.service.importfromdir.ImportInstitutionService;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

@Service
public class FileImportService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FileImportService.class);        

    private final StorageService storageService;
    
    List<ImportProcessor> importProcessors = new ArrayList<>();
    
    private long importInterval = 5000;

    private ThreadStatus status;

    public FileImportService(StorageService storageService,
            ImportInstitutionService importInstitutionService, ImportFundService importFundService,
            ImportDirectService importDirectService, ImportDaoService importDaoService,
            ImportArchDescService importArchDescService) throws IOException {
        this.storageService = storageService;
        
        // TODO: Make services as self registered
        registerImportProcessor(importDirectService);
        registerImportProcessor(importInstitutionService);
        registerImportProcessor(importFundService);
        registerImportProcessor(importArchDescService);
        registerImportProcessor(importDaoService);
        
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
    		importProcessor.importData(ic);
            if(ic.isFailed()||ic.getNumProcessed()>0) {
            	return;
            }
    	}
    }

    public void run() {
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

	public void registerImportProcessor(ImportProcessor importProcessor) {
	    for(int i=0; i<importProcessors.size(); i++) {
	        var p = importProcessors.get(i);
	        if(importProcessor.getPriority()>p.getPriority()) {
	            importProcessors.add(0, importProcessor);
	            return;
	        }		
	    }
	    importProcessors.add(importProcessor);
	}

}
