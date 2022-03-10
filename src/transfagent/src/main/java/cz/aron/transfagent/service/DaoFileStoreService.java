package cz.aron.transfagent.service;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import cz.aron.transfagent.config.ConfigDaoFileStore;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.service.DaoImportService.DaoImporter;
import cz.aron.transfagent.service.importfromdir.TransformService;


/**
 * Sluzba zpristupnujici Dao ulozene na disku
 */
@Service
public class DaoFileStoreService implements DaoImporter {
	
	private static Logger log = LoggerFactory.getLogger(DaoFileStoreService.class);
	
	private final ConfigDaoFileStore config;
	
	private final TransformService transformService;
	
	public DaoFileStoreService(ConfigDaoFileStore config, TransformService transformService) {
		this.config = config;
		this.transformService = transformService;
	}

	/**
	 * Vrati cestu k adresari s Dao nebo null pokud pro zadane id Dao neexistuje
	 * 
	 * @param id identifikator Dao
	 * @return Path nebo null pokud Dao neexistuje
	 */
	public Path getDaoDir(String id) {
		Path daoDir = config.getPath().resolve(id);
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
			if (!transformService.transform(dataDir)) {
				// delete empty dir
			}
		} catch (Exception e) {
			log.error("Fail to import dao {}", dao.getUuid(), e);
		}		
	}

}
