package cz.aron.transfagent.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.config.ConfigDaoFileStore;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.TransformService;


/**
 * Sluzba zpristupnujici Dao ulozene na disku. Seznam souboru je v koreni v souboru data.csv ve formatu (uuid;cesta)
 */
@ConditionalOnProperty(value="filestore.path")
@Service
public class DaoFileStoreService implements DaoImporter {
	
	private static Logger log = LoggerFactory.getLogger(DaoFileStoreService.class);
	
	private final ConfigDaoFileStore config;
	
	private final TransformService transformService;
	
	private final Map<String,Path> uuidToPath;
	
	private FileTime lastModification = null;
	
	private long lastCheck = 0;
	
	public DaoFileStoreService(ConfigDaoFileStore config, TransformService transformService) throws IOException {
		this.config = config;
		this.transformService = transformService;
		this.uuidToPath = init();
	}

	/**
	 * Vrati cestu k adresari s Dao nebo null pokud pro zadane id Dao neexistuje
	 * 
	 * @param id identifikator Dao
	 * @return Path nebo null pokud Dao neexistuje
	 */
	public synchronized Path getDaoDir(String id) {		
		checkMapping();		
		Path relativePath = uuidToPath.get(id);
		if (relativePath == null) {
			return null;
		}
		Path daoDir = config.getPath().resolve(relativePath);
		if (Files.isDirectory(daoDir)) {
			return daoDir;
		} else {
			return null;
		}
	}

	@Override
	public String getName() {
		return "file";
	}

	@Override
	public void importDaoFile(Dao dao, Path daoDir) {
		Path dataDir = getDaoDir(dao.getUuid().toString());				
		try {
			FileSystemUtils.copyRecursively(dataDir, daoDir);
			if (!transformService.transform(daoDir)) {
				// delete empty dir
			}
			dao.setState(DaoState.READY);
		} catch (Exception e) {
			log.error("Fail to import dao {}", dao.getUuid(), e);
		}		
	}
	
	private Map<String, Path> init() throws IOException {
		Map<String, Path> map = new HashMap<>();
		if (config.getPath() != null) {
			Path mappingFile = config.getPath().resolve("data.csv");
			if (!Files.isRegularFile(mappingFile)) {
				log.warn("File data.csv not found.");
				return map;
			}
			lastModification = Files.getLastModifiedTime(mappingFile);
			Reader in = new FileReader(mappingFile.toFile());
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').setTrim(true).build().parse(in);
			for (CSVRecord record : records) {
				String uuid = record.get(0);
				String path = record.get(1);
				map.put(uuid, Paths.get(path));
			}
			log.info("File data.csv read, num mappings:",map.size());
		}		
		return map;
	}
	
	private void checkMapping() {
		if (config.getPath() != null && (System.currentTimeMillis() - lastCheck) > 5000) {
			lastCheck = System.currentTimeMillis();
			Path mappingFile = config.getPath().resolve("data.csv");
			if (!Files.isRegularFile(mappingFile)) {
				return;
			}
			try {
				FileTime lastMod = Files.getLastModifiedTime(mappingFile);
				if (lastMod.compareTo(lastModification) != 0) {
					uuidToPath.clear();
					uuidToPath.putAll(init());
					log.info("Dao mapping updated from data.csv");
				}
			} catch (IOException ioEx) {
				log.error("Fail to update mapping from data.csv", ioEx);
			}			
		}
	}

}
