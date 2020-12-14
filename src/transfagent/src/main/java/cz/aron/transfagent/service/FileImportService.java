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
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
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
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.InstitutionRepository;

@Service
public class FileImportService implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FileImportService.class);

    private final ApuSourceRepository apuSourceRepository;
    
    private final CoreQueueRepository coreQueueRepository;
    
    private final InstitutionRepository institutionRepository;
    
    private final TransactionTemplate transactionTemplate;
    
    private final StorageService storageService;
    
    private ThreadStatus status;
    
	public FileImportService(ApuSourceRepository apuSourceRepository, CoreQueueRepository coreQueueRepository,
			InstitutionRepository institutionRepository,
			TransactionTemplate transactionTemplate, StorageService storageService) {
		this.apuSourceRepository = apuSourceRepository;
		this.coreQueueRepository = coreQueueRepository;
		this.institutionRepository = institutionRepository;
		this.transactionTemplate = transactionTemplate;
		this.storageService = storageService;
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
    	
    }
    
    private void processInstitutionsFolder(Path path) {
    	
    	List<Path> dirs;
    	try {
			 dirs = getOrderedDirectories(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
    	
    	for(Path dir:dirs) {
    		
    		List<Path> xmls;    		
			try (var stream = Files.list(dir)) {
				 xmls = stream
						.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("institution")
								&& f.getFileName().toString().endsWith(".xml"))
						.collect(Collectors.toList());								
			} catch (IOException ioEx) {
				throw new UncheckedIOException(ioEx);
			}
			
			Optional<Path> inst = xmls.stream().filter(p -> p.getFileName().toString().startsWith("institution-")
					&& p.getFileName().toString().endsWith(".xml")).findFirst();
			
			if (inst.isEmpty()) {
				log.warn("Directory is empty {}", dir);
				return;
			}
			
			String fileName = inst.get().getFileName().toString();			
			String tmp = fileName.substring("institution-".length());
			String code = tmp.substring(0,tmp.length()-".xml".length());
			
    		ImportInstitution ii = new ImportInstitution();
    		ApuSourceBuilder apusrcBuilder;
    		
			try {
				apusrcBuilder = ii.importInstitution(inst.get(), code);
			} catch (IOException e1) {
				throw new UncheckedIOException(e1);
			} catch (JAXBException e1) {
				throw new IllegalStateException(e1);
			}
    		
    		Institution institution = institutionRepository.findByCode(code);
    		if (institution!=null) {
    			apusrcBuilder.getApusrc().setUuid(institution.getApuSource().getUuid().toString());
    			apusrcBuilder.getApusrc().getApus().getApu().get(0).setUuid(institution.getUuid().toString());
    		}
    		
    		Path dataDir;
    		try(OutputStream fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
    			apusrcBuilder.build(fos);
    			
    		} catch (IOException ioEx) {
    			throw new UncheckedIOException(ioEx);
    		} catch (JAXBException e) {
				throw new IllegalStateException(e);
			}
    		
    		try {
				dataDir = storageService.moveToDataDir(dir);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}

    		if (institution == null) {    			
    			// instituce neexistuje, vytvorim novou    			
    			cz.aron.transfagent.domain.ApuSource apuSource = new cz.aron.transfagent.domain.ApuSource();
    			apuSource.setOrigDir(dir.getFileName().toString());
    			apuSource.setDataDir(dataDir.toString());
    			apuSource.setSourceType(SourceType.INSTITUTION);
    			apuSource.setUuid(UUID.fromString(apusrcBuilder.getApusrc().getUuid()));
    			apuSource.setDeleted(false);
    			apuSource.setDateImported(ZonedDateTime.now());
    			
    			Institution newInstitution = new Institution();
    			newInstitution.setApuSource(apuSource);
    			newInstitution.setCode(code);
    			newInstitution.setSource("source");    			
    			newInstitution.setUuid(UUID.fromString(apusrcBuilder.getApusrc().getApus().getApu().get(0).getUuid()));
    			
    			CoreQueue coreQueue = new CoreQueue();
    			coreQueue.setApuSource(apuSource);
    			transactionTemplate.execute(t -> {
    				apuSourceRepository.save(apuSource);
    				institutionRepository.save(newInstitution);
    				coreQueueRepository.save(coreQueue);
    				return null;
    			});    			
    		} else {    			
    			// aktualizace, pouze zmenim datovy adresar
    			cz.aron.transfagent.domain.ApuSource apuSource = institution.getApuSource();
    			apuSource.setDataDir(dataDir.toString());
    			apuSource.setOrigDir(dir.getFileName().toString());

    			CoreQueue coreQueue = new CoreQueue();
    			coreQueue.setApuSource(apuSource);
    			transactionTemplate.execute(t -> {
    				apuSourceRepository.save(apuSource);
    				institutionRepository.save(institution);
    				coreQueueRepository.save(coreQueue);
    				return null;
    			});    			
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

    /**
     * Přesunutí adresáře se soubory do jiného adresáře
     * 
     * @param source
     * @param target
     * @throws IOException
     */
    private void moveFolderTo(Path source, Path target) throws IOException {
        Path targetFolder = target.resolve(source.getFileName());
        if (Files.exists(targetFolder)) {
            String dateTime = new SimpleDateFormat("_yyyy_MM_dd_HH_mm_ss").format(new Date());
            targetFolder = Path.of(target.toString() + dateTime);
        }
        Files.move(source, targetFolder, StandardCopyOption.REPLACE_EXISTING);
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
