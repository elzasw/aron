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

    final private String DIRECT_DIR = "direct";

    final private String INSTITUTIONS_DIR = "institutions";

    final private String FUND_DIR = "fund";

    final private String ARCHDESC_DIR = "archdesc";

    final private String COLLECTION_DIR = "collection";

    final private String FINDINGAIDS_DIR = "findingAids";

    final private String DAO_DIR = "dao";

    final private String inputDirs[] = { DIRECT_DIR, INSTITUTIONS_DIR, FUND_DIR, ARCHDESC_DIR, COLLECTION_DIR, FINDINGAIDS_DIR, DAO_DIR };

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
    	Path inputPath = storageService.getInputPath();
    	for (String dir : inputDirs) {
    		createDirIfNotExists(inputPath.resolve(dir));
    	}
    	createDirIfNotExists(storageService.getDataPath());
    	createDirIfNotExists(storageService.getErrorPath());
    	createDirIfNotExists(storageService.getDaoPath());
    }

    /**
     * Monitorování vstupního adresáře
     * 
     * @throws IOException
     */
    private void importFile() throws IOException {

    	Path inputPath = storageService.getInputPath();

        var directPath = inputPath.resolve(DIRECT_DIR);
        processDirectFolder(directPath);

        var institutionsPath = inputPath.resolve(INSTITUTIONS_DIR);
        processInstitutionsFolder(institutionsPath);

        var fundsPath = inputPath.resolve(FUND_DIR);
        processFundsFolder(fundsPath);

        var findingAidsPath = inputPath.resolve(FINDINGAIDS_DIR);
        processFindingAidsFolder(findingAidsPath);

        var archDescPath = inputPath.resolve(ARCHDESC_DIR);
        processArchDescsFolder(archDescPath);

        var collectionsPath = inputPath.resolve(COLLECTION_DIR);
        processCollectionsFolder(collectionsPath);

        var daoPath = inputPath.resolve(DAO_DIR);
        processDaoFolder(daoPath);

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
    	for (Path dir : dirs) {    		
    		if (!importDirectService.processDirectory(dir)) {
    			return;
    		}
    	}		
	}

    private void processInstitutionsFolder(Path path) {    	
    	List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
    	for (Path dir : dirs) {    		
    		if (!importInstitutionService.processDirectory(dir)) {
    			return;
    		}
    	}
    }

    private void processFundsFolder(Path path) {
    	List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}    	
    	for (Path dir : dirs) {    		
    		importFundService.processDirectory(dir);    		
    	}
    }

    private void processFindingAidsFolder(Path path) {
    	// TODO
    }
  
    private void processArchDescsFolder(Path path) {
    	List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
    	for (Path dir : dirs) {    		
    		importArchDescService.processDirectory(dir);    		
    	}
    }

    private void processCollectionsFolder(Path path) {
    	// TODO
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

    private void createDirIfNotExists(Path pathDir) throws IOException {
		if (Files.notExists(pathDir)) {
			Files.createDirectories(pathDir);
		}
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
