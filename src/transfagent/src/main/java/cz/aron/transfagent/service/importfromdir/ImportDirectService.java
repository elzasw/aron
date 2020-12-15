package cz.aron.transfagent.service.importfromdir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ApuSource;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.service.StorageService;

@Service
public class ImportDirectService {
	
	private static final Logger log = LoggerFactory.getLogger(ImportDirectService.class);
	
	private final StorageService storageService;
	
	private final TransactionTemplate transactionTemplate;
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final CoreQueueRepository coreQueueRepository;
	
	public ImportDirectService(StorageService storageService, TransactionTemplate transactionTemplate,
			ApuSourceRepository apuSourceRepository, CoreQueueRepository coreQueueRepository) {
		this.storageService = storageService;
		this.transactionTemplate = transactionTemplate;
		this.apuSourceRepository = apuSourceRepository;
		this.coreQueueRepository = coreQueueRepository;
	}
	
	public boolean processDirectory(Path dir) {
		
		File[] files = dir.toFile().listFiles(new FilenameFilter() {
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
			Path storedToDir;
			try {
				storedToDir = storageService.moveToErrorDir(dir);
			} catch (IOException e) {
				log.error("Fail to move {} to error directory.",dir,e);
				throw new UncheckedIOException(e);
			}
			log.error("Folder {} doesn't contains apu.xml file, moved to error directory {}", dir.getFileName(),
					storedToDir);
			return true;
		}

		byte[] xml;
		try {
			xml = Files.readAllBytes(files[0].toPath());
		} catch (IOException e2) {
			log.error("Fail to read apusource {}",files[0]);
			throw new UncheckedIOException(e2);
		}
				
		ApuSource apux;
		try {
			apux = unmarshalApuSourceFromXml(xml);
		} catch (JAXBException | IOException e1) {
			Path storedToDir;
			try {
				storedToDir = storageService.moveToErrorDir(dir);
			} catch (IOException e) {
				log.error("Fail to move {} to error directory.",dir,e);
				throw new UncheckedIOException(e);
			}
			log.error("Fail to parse apu.xml. Dir {} moved to error directory {}", dir.getFileName(), storedToDir);
			return true;
		}
		
		Path dataDir;
		try {
			dataDir = storageService.moveToDataDir(dir);
		} catch (IOException e) {
			log.error("Fail to move {} to data dir.",dir,e);
			throw new UncheckedIOException(e);
		}
		
		transactionTemplate.execute(t -> {			
			var apuSource = new cz.aron.transfagent.domain.ApuSource();
			apuSource.setOrigDir(files[0].getName());
			apuSource.setDataDir(dataDir.toString());
			apuSource.setSourceType(SourceType.DIRECT);
			apuSource.setUuid(UUID.fromString(apux.getUuid()));
			apuSource.setDeleted(false);
			apuSource.setDateImported(ZonedDateTime.now());
			apuSource = apuSourceRepository.save(apuSource); 			
			CoreQueue coreQueue = new CoreQueue();
			coreQueue.setApuSource(apuSource);						
			coreQueueRepository.save(coreQueue);
			return null;
		});
		
		log.info("Direct imported uuid={}, directory={}",apux.getUuid(),dir.getFileName());		
		return true;
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


}
