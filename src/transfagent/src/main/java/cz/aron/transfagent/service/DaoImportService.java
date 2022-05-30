package cz.aron.transfagent.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.config.ConfigDao;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.DaoRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

@Service
public class DaoImportService implements ImportProcessor  {
	
	private static final Logger log = LoggerFactory.getLogger(DaoImportService.class);
	
	private final ApuSourceRepository apuSourceRepository;
	
	private final ConfigDao configDao;
	
	private final FileImportService importService;
	
	private final DaoRepository daoRepository;
	
	private final StorageService storageService;
	
	private final TransactionTemplate transactionTemplate;
	
	private final Map<String,DaoImporter> daoImporters;
	
	private Set<String> reportedMissingImporter;

	public DaoImportService(ApuSourceRepository apuSourceRepository, ConfigDao configDao,
			FileImportService importService, DaoRepository daoRepository, StorageService storageService,
			TransactionTemplate transactionTemplate, List<DaoImporter> daoImporters) {
		super();
		this.apuSourceRepository = apuSourceRepository;
		this.configDao = configDao;
		this.importService = importService;
		this.daoRepository = daoRepository;
		this.storageService = storageService;
		this.transactionTemplate = transactionTemplate;

		var importersMap = new HashMap<String, DaoImporter>();
		if (CollectionUtils.isNotEmpty(daoImporters)) {
			for (var daoImporter : daoImporters) {
				importersMap.put(daoImporter.getName(), daoImporter);
				log.info("Dao importer {}", daoImporter.getName());
			}
		}
		this.daoImporters = importersMap;
	}

    @PostConstruct
    void init() {
        importService.registerImportProcessor(this);
    }

	@Override
	public void importData(ImportContext ic) {
		
		if (daoImporters.isEmpty()) {
            return;
        }

		reportedMissingImporter = null;
		
		int numToSend = 0;
		if (configDao.getQueueSize()!=-1) {
			var numExistingToSend = daoRepository.countByStateAndTransferred(DaoState.READY, false);
			if (numExistingToSend!=null) {
				numToSend = numExistingToSend;
				if (numToSend>configDao.getQueueSize()) {
					log.info("Import dao paused, exists {} daos prepared to be sent", numToSend);
					return;
				}
			}
		}

		var daos = daoRepository.findTop1000ByStateOrderById(DaoState.ACCESSIBLE);		
		for (var dao : daos) {
			var daoImporter = daoImporters.get(dao.getSource());
			if (daoImporter == null) {
				logMissingImporter(dao.getSource());
				continue;
			}			
			var uuidStr = dao.getUuid().toString();
			Path daoPath;
			if (configDao.isUseSubdirs()) {
				var subdir = uuidStr.substring(6, 8);			
				daoPath = storageService.getDaoPath().resolve(subdir).resolve(uuidStr);
			} else {
				daoPath = storageService.getDaoPath().resolve(uuidStr);
			}
			try {
				Files.createDirectories(daoPath);
				daoImporter.importDaoFile(dao, daoPath);
				dao.setDataDir(dao.getUuid().toString());
			} catch (Exception e) {
				ic.setFailed(true);
				log.error("Dao file not imported: {}.", dao.getId(), e);
				return;
			}
			transactionTemplate.executeWithoutResult(c -> saveDao(dao));
			ic.addProcessed();
			
			numToSend++;
			if (numToSend>configDao.getQueueSize()) {
				log.info("Import dao paused, exists {} daos prepared to be sent", numToSend);
				return;
			}
		}

	}
	
    private void saveDao(Dao dao) {
        log.debug("Saving Dao to DB, daoId: {}", dao.getId());

        var dbDao = daoRepository.findById(dao.getId())
                .orElseThrow(
                    ()-> {
                        log.error("Dao not exists in DB, id: {}", dao.getId());
                        return new RuntimeException("Dao not exists");
                    }
                );
        dbDao.setDataDir(dao.getDataDir());
        if (dbDao.getUuid() == null) {
            // Dao has no uuid -> connected apusource have to be reindexed
            var apuSource = dbDao.getApuSource();
            apuSource.setReimport(true);
            apuSourceRepository.save(apuSource);
        }
        dbDao.setUuid(dao.getUuid());
        dbDao.setState(dao.getState());
        dbDao.setTransferred(false);
        dbDao.setDownload(dao.isDownload());
        daoRepository.save(dbDao);
    }
	
	private void logMissingImporter(String source) {
		if (reportedMissingImporter == null) {
			reportedMissingImporter = new HashSet<>();
		}
		if (reportedMissingImporter.contains(source)) {
			log.error("No dao importer for source {}", source);
		}
		reportedMissingImporter.add(source);
	}
	
	public static class DaoSource {

		private final String name;

		private final Set<DaoSourceRef> daoRefs;

		public DaoSource(String name, Set<DaoSourceRef> daoRefs) {
			super();
			this.name = name;
			this.daoRefs = daoRefs;
		}

		public String getName() {
			return name;
		}

		public Set<DaoSourceRef> getDaoRefs() {
			return daoRefs;
		}

	}	
	
	public static class DaoSourceRef {
		
		private final UUID uuid;
		
		private final String handle;
		
		public DaoSourceRef(UUID uuid, String handle) {
			this.uuid = uuid;
			this.handle = handle;
		}

		public UUID getUuid() {
			return uuid;
		}

		public String getHandle() {
			return handle;
		}

		@Override
		public int hashCode() {
			return Objects.hash(handle, uuid);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DaoSourceRef other = (DaoSourceRef) obj;
			return Objects.equals(handle, other.handle) && Objects.equals(uuid, other.uuid);
		}		
		
	}
	
	/**
	 * Aktualizuje Dao. Vytvori chybejici, deaktivuje nepristupne nebo jiz
	 * neexistujici. Existujici Dao se vyhledavaji podle handle
	 * 
	 * @param apuSource
	 * @param daoRefs   mapa zdroj seznam identifikatoru dao
	 */
    public void updateDaos(ApuSource apuSource, Collection<DaoSource> daoRefs) {
        var daos = daoRepository.findByApuSource(apuSource);
        Map<String, Dao> daoLookup = new HashMap<>();
        for (var dao : daos) {
            if(StringUtils.isNotBlank(dao.getHandle())) {
                daoLookup.put(dao.getHandle(), dao);
            }
        }

        for(var daoSource:daoRefs) {
        	for(var daoRef:daoSource.getDaoRefs()) {
        		var dao = daoLookup.get(daoRef.getHandle());
        		if (dao != null && Objects.equals(daoSource.getName(), dao.getSource())) {
        			if (dao.getState() == DaoState.INACCESSIBLE) {
                        dao.setState(DaoState.ACCESSIBLE);
                        daoRepository.save(dao);
                    }
                    daoLookup.remove(dao.getHandle());
        		} else {
        			// store new dao
                    dao = new Dao();
                    dao.setHandle(daoRef.getHandle());
                    dao.setApuSource(apuSource);
                    dao.setTransferred(false);
                    dao.setSource(daoSource.getName());
                    dao.setState(DaoState.ACCESSIBLE);                    
                    dao.setUuid(daoRef.getUuid());
                    daoRepository.save(dao);
        		}        		
        	}
        }

        // zbyle objekty musi byt zneplatneny
        for (var dao : daoLookup.values()) {
            dao.setState(DaoState.INACCESSIBLE);
            daoRepository.save(dao);
        }
    }
	
	    
    public interface DaoImporter {
    	
    	public String getName();
    	
    	public void importDaoFile(Dao dao, Path daoDir);    		
    	
    }

}
