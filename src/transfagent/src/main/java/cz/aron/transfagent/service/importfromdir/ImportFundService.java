package cz.aron.transfagent.service.importfromdir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportFundInfo;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
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
        String fileName = "fund-" + fund.getCode() + ".xml";

        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
        ApuSourceBuilder apuSourceBuilder;
        var ifi = new ImportFundInfo();
        try {
            apuSourceBuilder = ifi.importFundInfo(apuDir.resolve(fileName), fund.getUuid(), databaseDataProvider);
            apuSourceBuilder.setUuid(apuSource.getUuid());
            try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
                apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }
        } catch (Exception e) {
            log.error("Fail to process downloaded {}, dir={}", fileName, apuDir, e);
            return Result.FAILED;
        }
        return Result.REIMPORTED;
    }

    
    public interface FundImporter {
    	
    	enum ImportResult {
    		IMPORTED,
    		FAIL,
    		UNSUPPORTED
    	}
    	
    	ImportResult processPath(Path path);
    	
    }
    
}
