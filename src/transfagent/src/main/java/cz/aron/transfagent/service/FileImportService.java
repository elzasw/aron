package cz.aron.transfagent.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.service.importfromdir.ImportArchDescService;
import cz.aron.transfagent.service.importfromdir.ImportDaoService;
import cz.aron.transfagent.service.importfromdir.ImportDirectService;
import cz.aron.transfagent.service.importfromdir.ImportFundService;
import cz.aron.transfagent.service.importfromdir.ImportInstitutionService;

@Service
public class FileImportService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FileImportService.class);

    private enum DataDirs {
    	direct, institutions, funds, archdesc, collection, faindingAids, dao
    }

    private final StorageService storageService;

    private final ImportInstitutionService importInstitutionService;

    private final ImportFundService importFundService;

    private final ImportDirectService importDirectService;

    private final ImportDaoService importDaoService;

    private final ImportArchDescService importArchDescService;

    private ThreadStatus status;

    public FileImportService(StorageService storageService,
            ImportInstitutionService importInstitutionService, ImportFundService importFundService,
            ImportDirectService importDirectService, ImportDaoService importDaoService,
            ImportArchDescService importArchDescService) throws IOException {
        this.storageService = storageService;
        this.importInstitutionService = importInstitutionService;
        this.importFundService = importFundService;
        this.importDirectService = importDirectService;
        this.importDaoService = importDaoService;
        this.importArchDescService = importArchDescService;
        initDirs();
    }

    /**
     * Inicializace pracovních adresářů
     * 
     * @throws IOException
     */
    private void initDirs() throws IOException {
    	Path input = storageService.getInputPath();
    	for (DataDirs dir : DataDirs.values()) {
    		Path itemDir = input.resolve(dir.name());
    		if (Files.notExists(itemDir)) {
    			Files.createDirectories(itemDir);
			}
    	}
    }

    /**
     * Monitorování vstupního adresáře
     * 
     * @throws IOException
     */
    private void importFile() throws IOException {

    	Path inputPath = storageService.getInputPath();

        var institutionsPath = inputPath.resolve(DataDirs.institutions.name());
        processInstitutionsFolder(institutionsPath);

        var fundsPath = inputPath.resolve(DataDirs.funds.name());
        processFundsFolder(fundsPath);

        var findingAidsPath = inputPath.resolve(DataDirs.faindingAids.name());
        processFindingAidsFolder(findingAidsPath);

        var archDescPath = inputPath.resolve(DataDirs.archdesc.name());
        processArchDescsFolder(archDescPath);

        var collectionsPath = inputPath.resolve(DataDirs.collection.name());
        processCollectionsFolder(collectionsPath);

        var daoPath = inputPath.resolve(DataDirs.dao.name());
        processDaoFolder(daoPath);

        var directPath = inputPath.resolve(DataDirs.direct.name());
        processDirectFolder(directPath);
    }

    private void processDaoFolder(Path path) {
    	List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}    	
    	for(Path dir:dirs) {    		
    		importDaoService.processDirectory(dir);    		
    	}
    }
    
    private void processCollectionsFolder(Path path) {
    	
    }
    
    private void processArchDescsFolder(Path path) {
    	List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
    	
    	for(Path dir:dirs) {    		
    		importArchDescService.processDirectory(dir);    		
    	}
    }

    private void processFindingAidsFolder(Path path) {
    	
    }

    private void processFundsFolder(Path path) {
    	List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}    	
    	for(Path dir:dirs) {    		
    		importFundService.processDirectory(dir);    		
    	}
    }
    
    private void processInstitutionsFolder(Path path) {    	
    	List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
    	
    	for(Path dir:dirs) {    		
    		if (!importInstitutionService.processDirectory(dir)) {
    			return;
    		}
    	}
    }
    
    /**
     * Zpracování adresářů v adresáři direct
     *
     * @param path adresář direct
     * @return true nebo false
     */
	private void processDirectFolder(Path path) throws IOException {		
		List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

    	for(Path dir:dirs) {    		
    		if (!importDirectService.processDirectory(dir)) {
    			return;
    		}
    	}		
	}

    public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                importFile();
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("Error in import file. ", e);
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
    
	private List<Path> getOrderedDirectories(Path path) throws IOException {
		try (var stream = Files.list(path)) {
			List<Path> directories = stream.filter(f -> Files.isDirectory(f))
					.collect(Collectors.toCollection(ArrayList::new));
			directories.sort((p1, p2) -> p1.compareTo(p2));
			return directories;
		}
	}
    
}
