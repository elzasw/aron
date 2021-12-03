package cz.aron.transfagent.service.importfromdir;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportGeoService.GeoImporter.ImportResult;

@Service
public class ImportGeoService  extends ImportDirProcessor implements ReimportProcessor {
	
    private final ReimportService reimportService;
    
    private final FileImportService fileImportService;
    
    private final StorageService storageService;
    
    private final List<GeoImporter> geoImporters;
    
    private final String GEOS_DIR = "geos";
    
    public ImportGeoService(ReimportService reimportService, FileImportService fileImportService,
			StorageService storageService, List<GeoImporter> geoImporters) {
		this.reimportService = reimportService;
		this.fileImportService = fileImportService;
		this.storageService = storageService;
		this.geoImporters = geoImporters;
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
		return storageService.getInputPath().resolve(GEOS_DIR);
	}

	@Override
	protected boolean processDirectory(Path dir) {
		boolean imported = false;
    	out:for(var geoImporter:geoImporters) {    		
    		ImportResult importResult = geoImporter.processPath(dir);
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
	
	public interface GeoImporter {
		
    	enum ImportResult {
    		IMPORTED,
    		FAIL,
    		UNSUPPORTED
    	}
    	
    	ImportResult processPath(Path path);
    	
    	Result reimport(ApuSource apuSource);    	

	}	

}
