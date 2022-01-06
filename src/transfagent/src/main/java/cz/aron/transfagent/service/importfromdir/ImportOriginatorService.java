package cz.aron.transfagent.service.importfromdir;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.ArchivalEntity;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportOriginatorService.OriginatorImporter.ImportResult;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;

// TODO neni to to same jako ImportArchDescService ?

@Service
public class ImportOriginatorService extends ImportDirProcessor implements ReimportProcessor {
	
	private static final Logger log = LoggerFactory.getLogger(ImportOriginatorService.class);
	
    private final ReimportService reimportService;
    
    private final FileImportService fileImportService;
    
    private final StorageService storageService;
    
    private final List<OriginatorImporter> originatorImporters;
    
    private final String ORIGINATOR_DIR = "originators";
    
    private final ArchivalEntityRepository archivalEntityRepository;
	
	public ImportOriginatorService(ReimportService reimportService, FileImportService fileImportService,
			ArchivalEntityRepository archivalEntityRepository, StorageService storageService,
			List<OriginatorImporter> originatorImporters) {
		super();
		this.reimportService = reimportService;
		this.fileImportService = fileImportService;
		this.storageService = storageService;
		this.originatorImporters = originatorImporters;
		this.archivalEntityRepository = archivalEntityRepository;
	}

	@PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
        fileImportService.registerImportProcessor(this);
    }	

	@Override
	public int getPriority() {
		return 9;
	}

	@Override
	public Result reimport(ApuSource apuSource) {
		if (apuSource.getSourceType() != SourceType.ARCH_ENTITY)
			return Result.UNSUPPORTED;

		var archivalEntity = archivalEntityRepository.findByApuSource(apuSource);
		if (archivalEntity == null) {
			log.error("Missing archivalEntity: {}", apuSource.getId());
			return Result.UNSUPPORTED;
		}

		var ret = Result.UNSUPPORTED;
		var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
		for (var originatorImporter : originatorImporters) {
			var fiRet = originatorImporter.reimport(apuSource, archivalEntity, apuDir);
			if (fiRet != Result.UNSUPPORTED) {
				ret = fiRet;
				break;
			}
		}
		return ret;
	}

	@Override
	protected Path getInputDir() {
		return storageService.getInputPath().resolve(ORIGINATOR_DIR);
	}

	@Override
	protected boolean processDirectory(Path dir) {
    	boolean imported = false;
    	out:for(var originatorImporter:originatorImporters) {    		
    		ImportResult importResult = originatorImporter.processPath(dir);
    		switch(importResult) {
    		case IMPORTED:
    			imported = true;
    			break out;
    		case FAIL:
    			break out;
    		case UNSUPPORTED:
    			break;
    		default:
    		}
    	}
    	return imported;
	}

	public interface OriginatorImporter {
		
    	enum ImportResult {
    		IMPORTED,
    		FAIL,
    		UNSUPPORTED
    	}
    	
    	ImportResult processPath(Path path);
    	
    	Result reimport(ApuSource apuSource, ArchivalEntity archivalEntity, Path apuPath);

	}

}
