package cz.aron.transfagent.service.importfromdir;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFindingAidService.FindingAidImporter.ImportResult;

@Service
public class ImportFindingAidService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportFindingAidService.class);

    private final FileImportService fileImportService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final FindingAidRepository findingAidRepository;
        
    private final String FINDING_AID = "findingaid";

    private final String FINDING_AIDS_DIR = FINDING_AID + "s";
    
    private final List<FindingAidImporter> findingAidImporters;

	public ImportFindingAidService(FileImportService fileImportService, ReimportService reimportService,
			StorageService storageService, FindingAidRepository findingAidRepository,
			List<FindingAidImporter> findingAidImporters) {
		this.fileImportService = fileImportService;
		this.reimportService = reimportService;
		this.storageService = storageService;
		this.findingAidRepository = findingAidRepository;
		this.findingAidImporters = findingAidImporters;
	}

    @PostConstruct
    void register() {
        fileImportService.registerImportProcessor(this);
        reimportService.registerReimportProcessor(this);
    }

    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(FINDING_AIDS_DIR);
    }
    
    @Override
    public boolean processDirectory(Path dir) {
    	boolean imported = false;
    	out:for(var findingAidImporter:findingAidImporters) {    		
    		ImportResult importResult = findingAidImporter.processPath(dir);
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
    
    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.FINDING_AID)
            return Result.UNSUPPORTED;
        var findingAid = findingAidRepository.findByApuSource(apuSource);
        if (findingAid == null) {
            log.error("Missing fund: {}", apuSource.getId());
            return Result.UNSUPPORTED;
        }
        var ret = Result.UNSUPPORTED;        
        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        for(var findingAidImporter:findingAidImporters) {        	
        	var fiRet = findingAidImporter.reimport(apuSource, findingAid, apuDir);
        	if (fiRet!=Result.UNSUPPORTED) {
        		ret = fiRet;
        		break;
        	}
        }        
        return ret;
    }
    
    public interface FindingAidImporter {
    	
    	enum ImportResult {
    		IMPORTED,
    		FAIL,
    		UNSUPPORTED
    	}
    	
    	ImportResult processPath(Path path);
    	
    	/**
    	 * Reimport apu 
    	 * @param apuSource nacteny objekt ApuSource z databaze, ktery ma byt aktualizovan
    	 * @param findingAid nacteny objekt FindingAid
    	 * @param apuPath cesta k adresari/zipu s daty apu
    	 * @return vysledek reimportu
    	 */
    	Result reimport(ApuSource apuSource, FindingAid findingAid, Path apuPath); 
    	
    }


}
