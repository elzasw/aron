package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Attachment;
import cz.aron.transfagent.domain.FindingAid;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.ead3.ImportFindingAidInfo;
import cz.aron.transfagent.peva.ImportPevaFindingAid;
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
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFindingAidService extends ImportDirProcessor implements ReimportProcessor {

    private static final Logger log = LoggerFactory.getLogger(ImportFindingAidService.class);

    private final ApuSourceService apuSourceService;

    private final FileImportService fileImportService;

    private final ReimportService reimportService;

    private final StorageService storageService;

    private final InstitutionRepository institutionRepository;

    private final FindingAidRepository findingAidRepository;

    private final AttachmentRepository attachmentRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final FundRepository fundRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final TransactionTemplate transactionTemplate;

    private final ConfigurationLoader configurationLoader;
    
    private final FindingAidService findingAidService;
    
    // TODO predelat jednotlive importy do bean
    private final Optional<Peva2Import> peva2import;

    private final String FINDING_AID = "findingaid";

    private final String FINDING_AID_DASH = FINDING_AID + "-";

    private final String FINDING_AIDS_DIR = FINDING_AID + "s";
    
    private final String PEVA_FINDING_AID = "pevafa";
    
    private final String PEVA_FINDING_AID_DASH = PEVA_FINDING_AID + "-";
    
    private final Tika tika = new Tika();

    private ImportProtocol protocol;

    public ImportFindingAidService(ApuSourceService apuSourceService, FileImportService fileImportService,
            ReimportService reimportService, StorageService storageService, InstitutionRepository institutionRepository,
            FindingAidRepository findingAidRepository, AttachmentRepository attachmentRepository, ApuSourceRepository apuSourceRepository,
            CoreQueueRepository coreQueueRepository, FundRepository fundRepository,
            DatabaseDataProvider databaseDataProvider, TransactionTemplate transactionTemplate,
            ConfigurationLoader configurationLoader, Optional<Peva2Import> peva2import, FindingAidService findingAidService) {
        this.apuSourceService = apuSourceService;
        this.fileImportService = fileImportService;
        this.reimportService = reimportService;
        this.storageService = storageService;
        this.institutionRepository = institutionRepository;
        this.findingAidRepository = findingAidRepository;
        this.attachmentRepository = attachmentRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.fundRepository = fundRepository;
        this.databaseDataProvider = databaseDataProvider;
        this.transactionTemplate = transactionTemplate;
        this.configurationLoader = configurationLoader;
        this.peva2import = peva2import;
        this.findingAidService = findingAidService;
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

    /**
     * Zpracování adresářů s archivní pomucky
     * 
     * @param dir zpracovavany adresar
     */
    @Override
    public boolean processDirectory(Path dir) {
		List<Path> xmls;
		try (var stream = Files.list(dir)) {
			xmls = stream.filter(f -> Files.isRegularFile(f)
					&& (f.getFileName().toString().startsWith(FINDING_AID)
							|| f.getFileName().toString().startsWith(PEVA_FINDING_AID))
					&& f.getFileName().toString().endsWith(".xml")).collect(Collectors.toList());
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
        }

        var findingaidXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(FINDING_AID_DASH)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();
        
        var pevaFindingAidXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst(); 

        if (findingaidXml.isEmpty()&&pevaFindingAidXml.isEmpty()) {
            log.warn("Directory is empty {}", dir);
            return false;
        }

        protocol = new ImportProtocol(dir);
        protocol.add("Zahájení importu");
        
        boolean result;
        if (findingaidXml.isPresent()) {
            var findingaidXmlPath = findingaidXml.get();
            var fileName = findingaidXmlPath.getFileName().toString();
            var tmp = fileName.substring(FINDING_AID_DASH.length());
            var findingaidCode = tmp.substring(0, tmp.length() - ".xml".length());
            result = transactionTemplate.execute(t -> importFindingAid(t, dir, findingaidCode, findingaidXmlPath));        	
        } else {
            var findingaidXmlPath = pevaFindingAidXml.get();
            var fileName = findingaidXmlPath.getFileName().toString();
            var tmp = fileName.substring(PEVA_FINDING_AID_DASH.length());
            var findingaidCode = tmp.substring(0, tmp.length() - ".xml".length());
            result = transactionTemplate.execute(t -> importFindingAidPeva(t, dir, findingaidCode, findingaidXmlPath));        	        	
        }
        protocol.add("Import byl úspěšně dokončen");
        return result;
    }

    private boolean importFindingAidPeva(TransactionStatus t, 
            Path dir, String findingAidCode, 
            Path findingAidXmlPath) {
            
        var ifai = new ImportPevaFindingAid(peva2import.get().getCodeLists());
        ApuSourceBuilder builder;
        try {
            builder = ifai.importFindingAidInfo(findingAidXmlPath, databaseDataProvider);
        } catch (IOException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        }

        var funds = new ArrayList<Fund>();
        for (var fundUUID:ifai.getFundUUIDs()) {
        	var fund = fundRepository.findByUuid(fundUUID);
        	if (fund == null) {
                protocol.add("The entry Fund code={" + findingAidCode + "} must exist.");
                //throw new IllegalStateException("The entry Fund code={" + findingAidCode + "} must exist.");
            }
        	funds.add(fund);
        }
        var findingAid = findingAidRepository.findByUuid(ifai.getFindingAidUUID());
        UUID findingaidUuid, apusourceUuid;
        if (findingAid != null) {
            findingaidUuid = findingAid.getUuid();
            apusourceUuid = findingAid.getApuSource().getUuid();
        } else {
            findingaidUuid = ifai.getFindingAidUUID();
            apusourceUuid = UUID.randomUUID();
        }

        var institutionCode = ifai.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
            protocol.add("The entry Institution code={" + institutionCode + "} must exist.");
            throw new IllegalStateException("The entry Institution code={" + institutionCode + "} must exist.");
        }

        List<Path> attachments = readAllAttachments(dir, builder);
        try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
            builder.setUuid(apusourceUuid);
            builder.build(fos, new ApuValidator(configurationLoader.getConfig()));
        } catch (IOException ioEx) {
            protocol.add("Chyba " + ioEx.getMessage());
            throw new UncheckedIOException(ioEx);
        } catch (JAXBException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
            protocol.setLogPath(storageService.getDataPath().resolve(dataDir));
            // change directory of attachments
            if(attachments!=null) {
                attachments = attachments.stream()
                        .map(p -> dataDir.resolve(p.getFileName()))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        
        if (findingAid == null) {
            findingAidService.createFindingAid(findingAidCode, funds, institution, dataDir, dir, builder, attachments);
        } else {
            findingAidService.updateFindingAid(findingAid, dataDir, dir, builder, attachments);
        }
        
    	return true;
    }
    
    private boolean importFindingAid(TransactionStatus t, 
                                  Path dir, String findingAidCode, 
                                  Path findingAidXmlPath) {
        var fund = fundRepository.findByCode(findingAidCode);
        if (fund == null) {
            protocol.add("The entry Fund code={" + findingAidCode + "} must exist.");
            throw new IllegalStateException("The entry Fund code={" + findingAidCode + "} must exist.");
        }

        var findingAid = findingAidRepository.findByCode(findingAidCode);
        UUID findingaidUuid, apusourceUuid;
        if (findingAid != null) {
            findingaidUuid = findingAid.getUuid();
            apusourceUuid = findingAid.getApuSource().getUuid();
        } else {
            findingaidUuid = UUID.randomUUID();
            apusourceUuid = UUID.randomUUID();
        }

        var ifai = new ImportFindingAidInfo(findingAidCode);
        ApuSourceBuilder builder;

        try {
            builder = ifai.importFindingAidInfo(findingAidXmlPath, findingaidUuid, databaseDataProvider);
        } catch (IOException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        }

        var institutionCode = ifai.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
            protocol.add("The entry Institution code={" + institutionCode + "} must exist.");
            throw new IllegalStateException("The entry Institution code={" + institutionCode + "} must exist.");
        }

        List<Path> attachments = readAttachments(dir, findingAidCode, builder);

        try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
            builder.setUuid(apusourceUuid);
            builder.build(fos, new ApuValidator(configurationLoader.getConfig()));
        } catch (IOException ioEx) {
            protocol.add("Chyba " + ioEx.getMessage());
            throw new UncheckedIOException(ioEx);
        } catch (JAXBException e) {
            protocol.add("Chyba " + e.getMessage());
            throw new IllegalStateException(e);
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
            protocol.setLogPath(storageService.getDataPath().resolve(dataDir));
            // change directory of attachments
            if(attachments!=null) {
                attachments = attachments.stream()
                        .map(p -> dataDir.resolve(p.getFileName()))
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        
        if (findingAid == null) {
            findingAidService.createFindingAid(findingAidCode, Collections.singletonList(fund), institution, dataDir, dir, builder, attachments);
        } else {
            findingAidService.updateFindingAid(findingAid, dataDir, dir, builder, attachments);
        }
        return true;
    }
    
	private List<Path> readAllAttachments(Path dir, ApuSourceBuilder builder) {
		// get root apu
		var apu = builder.getMainApu();
		List<Path> attachments = new ArrayList<>();

		try (var stream = Files.list(dir)) {
			stream.forEach(f -> {
				if (Files.isRegularFile(f) && !f.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)
						&& !"protokol.txt".equals(f.getFileName().toString())) {

					String mimetype = null;
					try {
						mimetype = tika.detect(f);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					var att = builder.addAttachment(apu, f.getFileName().toString(), mimetype);
					attachments.add(f);
				}
			});
		} catch (IOException e) {
			log.error("Fail to read attachments from directory {}", dir);
			throw new UncheckedIOException(e);
		}
		return attachments;
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

}
