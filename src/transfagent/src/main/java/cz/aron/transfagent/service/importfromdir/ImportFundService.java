package cz.aron.transfagent.service.importfromdir;

import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFundService.FundImporter.ImportResult;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFundService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportFundService.class);

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final FundRepository fundRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;
    
    private final List<FundImporter> fundImporters;
        
    private final FileImportService fileImportService;

    private final String FUND_DIR = "fund";

    public ImportFundService(ReimportService reimportService, StorageService storageService,            
            FundRepository fundRepository, DatabaseDataProvider databaseDataProvider, TransactionTemplate transactionTemplate,
            ConfigurationLoader configurationLoader,
            FileImportService fileImportService, List<FundImporter> fundImporters) {
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.fundRepository = fundRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.configurationLoader = configurationLoader;
        this.fileImportService = fileImportService;
        this.fundImporters = fundImporters;
    }

    @PostConstruct
    void register() {
        reimportService.registerReimportProcessor(this);
        fileImportService.registerImportProcessor(this);
    }

    @Override
    public int getPriority() { return 8; }
    
    @Override
    protected Path getInputDir() {
        return storageService.getInputPath().resolve(FUND_DIR);
    }

    /**
     * Zpracování adresářů s archivními soubory
     * 
     * @param dir zpracovavany adresar
     */
    @Override
    public boolean processDirectory(Path dir) {
    	
    	boolean imported = false;
    	out:for(var fundImporter:fundImporters) {    		
    		ImportResult importResult = fundImporter.processPath(dir);
    		switch(importResult) {
    		case IMPORTED:
    		case IGNORED:
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
        if (apuSource.getSourceType() != SourceType.FUND)
            return Result.UNSUPPORTED;
        var fund = fundRepository.findByApuSource(apuSource);
        if (fund == null) {
            log.error("Missing fund: {}", apuSource.getId());
            return Result.UNSUPPORTED;
        }
        var ret = Result.UNSUPPORTED;        
        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        for(var fundImporter:fundImporters) {        	
        	var fiRet = fundImporter.reimport(apuSource, fund, apuDir);
        	if (fiRet!=Result.UNSUPPORTED) {
        		ret = fiRet;
        		break;
        	}
        }        
        return ret;
    }

    
    public interface FundImporter {
    	
    	enum ImportResult {
    		IMPORTED,
    		FAIL,
    		UNSUPPORTED,
    		IGNORED
    	}
    	
    	/**
    	 * Import fondu ze souboru
    	 * @param path cesta k souboru
    	 * @return vysledek importu
    	 */
    	ImportResult processPath(Path path);
    	
    	/**
    	 * Reimport apu 
    	 * @param apuSource nacteny objekt ApuSource z databaze, ktery ma byt aktualizovan
    	 * @param fund nacteny objekt fond
    	 * @param apuPath cesta k adresari/zipu s daty apu
    	 * @return vysledek reimportu
    	 */
    	Result reimport(ApuSource apuSource, Fund fund, Path apuPath);
    	
    }
    
}
