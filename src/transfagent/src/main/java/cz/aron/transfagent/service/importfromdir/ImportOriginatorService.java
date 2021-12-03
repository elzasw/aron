package cz.aron.transfagent.service.importfromdir;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportOriginatorService.OriginatorImporter.ImportResult;

// TODO neni to to same jako ImportArchDescService ?

@Service
public class ImportOriginatorService extends ImportDirProcessor implements ReimportProcessor {
	
    private final ReimportService reimportService;
    
    private final FileImportService fileImportService;
    
    private final StorageService storageService;
    
    private final List<OriginatorImporter> originatorImporters;
    
    private final String ORIGINATOR_DIR = "originators";    
	
	public ImportOriginatorService(ReimportService reimportService, FileImportService fileImportService,
			StorageService storageService, List<OriginatorImporter> originatorImporters) {
		super();
		this.reimportService = reimportService;
		this.fileImportService = fileImportService;
		this.storageService = storageService;
		this.originatorImporters = originatorImporters;
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
		throw new UnsupportedOperationException("Not implemented yet");
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
    	
    	Result reimport(ApuSource apuSource);    	

	}	
	
}
