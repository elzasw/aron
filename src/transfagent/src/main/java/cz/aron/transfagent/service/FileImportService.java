package cz.aron.transfagent.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ApuSource;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportInstitution;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.importfromdir.ImportFundService;
import cz.aron.transfagent.service.importfromdir.ImportInstitutionService;

@Service
public class FileImportService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FileImportService.class);

    private final ApuSourceRepository apuSourceRepository;
    
    private final CoreQueueRepository coreQueueRepository;
    
    private final InstitutionRepository institutionRepository;
    
    private final TransactionTemplate transactionTemplate;
    
    private final StorageService storageService;
    
    private final ArchivalEntityRepository archivalEntityRepository;
    
    private final EntitySourceRepository entitySourceRepository;
    
    private final ImportInstitutionService importInstitutionService;
    
    private final ImportFundService importFundService;
    
    private ThreadStatus status;
    
	public FileImportService(ApuSourceRepository apuSourceRepository, CoreQueueRepository coreQueueRepository,
			InstitutionRepository institutionRepository, ArchivalEntityRepository archivalEntityRepository,
			EntitySourceRepository entitySourceRepository,
			TransactionTemplate transactionTemplate, StorageService storageService,
			ImportInstitutionService importInstitutionService, ImportFundService importFundService) {
		this.apuSourceRepository = apuSourceRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.institutionRepository = institutionRepository;
		this.archivalEntityRepository = archivalEntityRepository;
		this.entitySourceRepository = entitySourceRepository;
		this.transactionTemplate = transactionTemplate;
		this.storageService = storageService;
		this.importInstitutionService = importInstitutionService;
		this.importFundService = importFundService;
	}

    /**
     * Monitorování vstupního adresáře
     * 
     * @throws IOException
     */
    private void importFile() throws IOException {
    	
    	Path inputPath = storageService.getInputPath();
    	
        // kontrola, zda vstupní adresář existuje
        if (Files.notExists(inputPath)) {
            log.error("Input folder {} not exists.", inputPath);
            throw new RuntimeException("Input folder not exists.");
        }

        Path direct = inputPath.resolve("direct");

        // kontrola, zda adresář direct existuje
        if (Files.notExists(direct)) {
            log.error("Direct folder in input folder {} not exists.", direct);
            throw new RuntimeException("Direct folder in input folder not exists.");
        }

        processDirectFolder(direct);
        
        var institutionsPath = inputPath.resolve("institutions");
        processInstitutionsFolder(institutionsPath);
        
        var fundsPath = inputPath.resolve("funds");
        processFundsFolder(fundsPath);
        
        var findingAidsPath = inputPath.resolve("faindingAids");
        processFindingAidsFolder(findingAidsPath);
        
        var archDescPath = inputPath.resolve("archdesc");
        processArchDescsFolder(archDescPath);
        
        var collectionsPath = inputPath.resolve("collections");
        processCollectionsFolder(collectionsPath);
    }
    
    private void processCollectionsFolder(Path path) {
    	
    }
    
    private void processArchDescsFolder(Path path) {
    	
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
    		importInstitutionService.processDirectory(dir);    		
    	}
    }
    
    /**
     * Zpracování adresářů v adresáři direct
     *
     * @param path adresář direct
     * @return true nebo false
     */
	private void processDirectFolder(Path path) throws IOException {
		File[] dirs = path.toFile().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return dir.isDirectory();
			}
		});
		// pokud je adresář prázdný, není co zpracovávat
		if (dirs.length == 0) {
			return;
		}
		for (File dir : dirs) {
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					// process apux.xml or apux-XY.xml files
					if (name.startsWith("apux") && name.endsWith("xml")) {
						return true;
					}
					return false;
				}
			});

			if (files.length == 0 || files.length > 1) {
				Path storedToDir = storageService.moveToErrorDir(dir.toPath());
				log.error("Folder {} doesn't contains apu.xml file, moved to error directory {}", dir.getName(),
						storedToDir);
				continue;
			}

			byte[] xml = Files.readAllBytes(files[0].toPath());
			ApuSource apux;

			try {
				apux = unmarshalApuSourceFromXml(xml);
			} catch (JAXBException | IOException e1) {
				Path storedToDir = storageService.moveToErrorDir(dir.toPath());
				log.error("Fail to parse apu.xml. Dir {} moved to error directory {}", dir.getName(), storedToDir);
				continue;
			}

			Path dataDir = storageService.moveToDataDir(dir.toPath());
			cz.aron.transfagent.domain.ApuSource apuSource = new cz.aron.transfagent.domain.ApuSource();
			apuSource.setOrigDir(files[0].getName());
			apuSource.setDataDir(dataDir.toString());
			apuSource.setSourceType(SourceType.DIRECT);
			apuSource.setUuid(UUID.fromString(apux.getUuid()));
			apuSource.setDeleted(false);
			apuSource.setDateImported(ZonedDateTime.now());
			CoreQueue coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);
			transactionTemplate.execute(t -> {
				apuSourceRepository.save(apuSource);
				coreQueueRepository.save(coreQueue);
				return null;
			});

		}
	}

    /**
     * Vytváření objektů na základě XML souboru
     * 
     * @param xml
     * @return cz.aron.apux._2020.ApuSource
     * @throws JAXBException
     * @throws IOException
     */
    private ApuSource unmarshalApuSourceFromXml(byte[] xml) throws JAXBException, IOException {
        ApuSource apuSource = null;
        try (InputStream is = new ByteArrayInputStream(xml)) {
            Unmarshaller unmarshaller = ApuSourceBuilder.apuxXmlContext.createUnmarshaller();
            unmarshaller.setSchema(ApuSourceBuilder.schemaApux);
            apuSource = ((JAXBElement<ApuSource>) unmarshaller.unmarshal(is)).getValue();
        }
        return apuSource;
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
