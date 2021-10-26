package cz.aron.transfagent.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.common.BulkOperation;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.EntitySource;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.domain.IdProjection;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.repository.EntitySourceRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;

@Service
public class ArchivalEntityImportService implements /*SmartLifecycle,*/ ReimportProcessor, ImportProcessor {
	
	private static Logger log = LoggerFactory.getLogger(ArchivalEntityImportService.class);
	
	private final ArchivalEntityRepository archivalEntityRepository;
	
	private final FileImportService importService;
	
	private final ReimportService reimportService;
		
	private final Map<String, ArchivalEntityImporter> entityImportersMap = new HashMap<>();
	
	private final List<ArchivalEntityImporter> entityImporters;
	
	private final EntitySourceRepository entitySourceRepository;
	
	public ArchivalEntityImportService(ArchivalEntityRepository archivalEntityRepository,
			ReimportService reimportService, FileImportService importService, List<ArchivalEntityImporter> importers,
			EntitySourceRepository entitySourceRepository) {
		this.archivalEntityRepository = archivalEntityRepository;
		this.importService = importService;
		this.reimportService = reimportService;
		importers.forEach(importer -> {
			importer.importedClasses().forEach(cls -> {
				entityImportersMap.put(cls, importer);
			});
		});
		this.entityImporters = importers;
		this.entitySourceRepository = entitySourceRepository;
	}

	@PostConstruct
	void init() {
		reimportService.registerReimportProcessor(this);
		importService.registerImportProcessor(this);
	}

	@Override
	public Result reimport(ApuSource apuSource) {
		var ret = Result.UNSUPPORTED;
		for(var entityImporter:entityImporters) {
			ret = entityImporter.reimport(apuSource);
			if (!Result.UNSUPPORTED.equals(ret)) {
				break;
			}
		}
		if (Result.UNSUPPORTED.equals(ret)) {
			log.warn("Unsupported reimport id={}, uuid={}", apuSource.getId(), apuSource.getUuid());
		}
		return ret;
	}

    @Override
    public void importData(ImportContext ic) {
                
        var ids = archivalEntityRepository.findTop1000ByStatusOrderById(EntityStatus.ACCESSIBLE);
        for(var id: ids) {
        	var entityImporter = entityImportersMap.get(id.getEntityClass());
        	if (entityImporter!=null) {
        		try {
        			if (!entityImporter.importEntity(id)) {
        				ic.setFailed(false);
        			}
                } catch(Exception e) {
                    ic.setFailed(true);
                    log.error("Entity not imported: {}", id, e);
                    return;
                }
                ic.addProcessed();	
        	} else {
        		log.warn("No importer for id={}, class={}", id.getId(), id.getEntityClass());
        	}
        }
    }
    
    public interface ArchivalEntityImporter {
    	
    	List<String> importedClasses();
    	
    	boolean importEntity(IdProjection id);
    	
    	Result reimport(ApuSource apuSource);

    }

}
