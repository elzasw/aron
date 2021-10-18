package cz.aron.transfagent.service.importfromdir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.ead3.ImportFindingAidInfo;
import cz.aron.transfagent.peva.Peva2Import;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ApuSourceService;
import cz.aron.transfagent.service.FileImportService;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.ImportProtocol;
import cz.aron.transfagent.service.ReimportService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFindingAidService.FindingAidImporter.ImportResult;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFindingAidService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportFindingAidService.class);

    private final FileImportService fileImportService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final FindingAidRepository findingAidRepository;

    private final AttachmentRepository attachmentRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;
        
    private final String FINDING_AID = "findingaid";

    private final String FINDING_AID_DASH = FINDING_AID + "-";

    private final String FINDING_AIDS_DIR = FINDING_AID + "s";
    
    private final String PEVA_FINDING_AID = "pevafa";
    
    private final String PEVA_FINDING_AID_DASH = PEVA_FINDING_AID + "-";
    
    private final List<FindingAidImporter> findingAidImporters;

    private ImportProtocol protocol;

    public ImportFindingAidService(FileImportService fileImportService,
            ReimportService reimportService, StorageService storageService,
            FindingAidRepository findingAidRepository, AttachmentRepository attachmentRepository,
            DatabaseDataProvider databaseDataProvider,
            ConfigurationLoader configurationLoader,
            List<FindingAidImporter> findingAidImporters) {
        this.fileImportService = fileImportService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.findingAidRepository = findingAidRepository;
        this.attachmentRepository = attachmentRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.configurationLoader = configurationLoader;
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

    private List<Path> readAttachments(Path dir, String findingaidCode, ApuSourceBuilder builder) {
        // get root apu
        var apu = builder.getMainApu();

        List<Path> attachments = new ArrayList<>();
        
        var filePdf = this.getPdfPath(dir, findingaidCode);
        if(Files.exists(filePdf)) {
            String name = "Archivní pomůcka - "+findingaidCode+".pdf";

            var att = builder.addAttachment(apu, name, "application/pdf");

            attachments.add(filePdf);
        }
        return attachments;
    }

    private void updateAttachments(FindingAid findingAid, 
                                   ApuSourceBuilder builder, List<Path> attachments) {
        var dbAttachments = attachmentRepository.findByApuSource(findingAid.getApuSource());
        if(dbAttachments.size()>0) {
            // drop old attachment
            attachmentRepository.deleteInBatch(dbAttachments);
        }

        if(CollectionUtils.isNotEmpty(attachments)) {
            var mainApu = builder.getMainApu();
            Validate.isTrue(attachments.size()==mainApu.getAttchs().size(), 
                    "Attachment size does not match, attachments: %i, xml: %i",
                    attachments.size(),
                    mainApu.getAttchs().size());
            
            var attIt = mainApu.getAttchs().iterator();                        
            var fileIt = attachments.iterator();
            while(attIt.hasNext()&&fileIt.hasNext()) {
                var att = attIt.next();
                var attPath = fileIt.next();
                
                Validate.notNull(att.getFile(), "DaoFile is null");
                Validate.notNull(att.getFile().getUuid(), "DaoFile without UUID");
                
                createAttachment(findingAid.getApuSource(), 
                                 attPath.getFileName().toString(),
                                 UUID.fromString(att.getFile().getUuid()));
            }
        }
    }

    /**
     * Return standard name of PDF
     * @param parentPath
     * @param findingAidCode
     * @return Path
     */
    private Path getPdfPath(Path parentPath, String findingAidCode) {
        var filePdf = FINDING_AID_DASH + findingAidCode + ".pdf";
        return parentPath.resolve(filePdf);
    }

    // upravit i pro pevu
    @Override
    public Result reimport(ApuSource apuSource) {
        if (apuSource.getSourceType() != SourceType.FINDING_AID) {
            return Result.UNSUPPORTED;
        }
        var apuDir = storageService.getApuDataDir(apuSource.getDataDir());

        protocol = new ImportProtocol(apuDir);
        protocol.add("Zahájení reimportu");

        var findingAid = findingAidRepository.findByApuSource(apuSource);
        if (findingAid == null) {
            log.error("Missing findingAid: {}", apuSource.getId());
            protocol.add("Missing findingAid: " + apuSource.getId());
            return Result.UNSUPPORTED;
        }
        String fileXml = FINDING_AID_DASH + findingAid.getCode() + ".xml";

        ApuSourceBuilder builder;
        var ifai = new ImportFindingAidInfo(findingAid.getCode());
        try {
            builder = ifai.importFindingAidInfo(apuDir.resolve(fileXml), findingAid.getUuid(), databaseDataProvider);
            builder.setUuid(apuSource.getUuid());
            try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
                builder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }
            
            var attachments = readAttachments(apuDir, findingAid.getCode(), builder);
            updateAttachments(findingAid, builder, attachments);
            
        } catch (Exception e) {
            log.error("Fail to process downloaded {}, dir={}", fileXml, apuDir, e);
            protocol.add("Chyba " + e.getMessage());
            return Result.FAILED;
        }

        protocol.add("Reimport byl úspěšně dokončen");
        return Result.REIMPORTED;
    }

    private void createAttachment(ApuSource apuSource, String fileName, UUID uuid) {
        var attachment = new Attachment();
        attachment.setApuSource(apuSource);
        attachment.setFileName(fileName);
        attachment.setUuid(uuid);
        attachment = attachmentRepository.save(attachment); 
    }
    
    public interface FindingAidImporter {
    	
    	enum ImportResult {
    		IMPORTED,
    		FAIL,
    		UNSUPPORTED
    	}
    	
    	ImportResult processPath(Path path);
    	
    }


}
