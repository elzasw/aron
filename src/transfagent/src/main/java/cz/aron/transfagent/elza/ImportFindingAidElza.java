package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
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
import cz.aron.transfagent.ead3.ImportFindingAidInfo;
import cz.aron.transfagent.repository.AttachmentRepository;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.ImportProtocol;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFindingAidService.FindingAidImporter;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor.Result;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFindingAidElza implements FindingAidImporter {
	
	private static final Logger log = LoggerFactory.getLogger(ImportFindingAidElza.class);
	
    private static final String FINDING_AID = "findingaid";

    private static final String FINDING_AID_DASH = FINDING_AID + "-";
	
	private final FundRepository fundRepository;
	
	private final FindingAidRepository findingAidRepository;
	
	private final InstitutionRepository institutionRepository;
	
	private final StorageService storageService;
	
	private final FindingAidService findingAidService;
	
	private final DatabaseDataProvider databaseDataProvider;
	
	private final ConfigurationLoader configurationLoader;
	
	private final TransactionTemplate transactionTemplate;
	
	private final AttachmentRepository attachmentRepository;

	public ImportFindingAidElza(FundRepository fundRepository, FindingAidRepository findingAidRepository,
			InstitutionRepository institutionRepository, StorageService storageService,
			FindingAidService findingAidService, DatabaseDataProvider databaseDataProvider,
			ConfigurationLoader configurationLoader, TransactionTemplate transactionTemplate,
			AttachmentRepository attachmentRepository) {
		super();
		this.fundRepository = fundRepository;
		this.findingAidRepository = findingAidRepository;
		this.institutionRepository = institutionRepository;
		this.storageService = storageService;
		this.findingAidService = findingAidService;
		this.databaseDataProvider = databaseDataProvider;
		this.configurationLoader = configurationLoader;
		this.transactionTemplate = transactionTemplate;
		this.attachmentRepository = attachmentRepository;
	}

	@Override
	public ImportResult processPath(Path path) {
		if (!path.getFileName().toString().startsWith(FINDING_AID_DASH)) {
			return ImportResult.UNSUPPORTED;
		}

		List<Path> xmls;
		try (var stream = Files.list(path)) {
			xmls = stream.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith(FINDING_AID_DASH)
					&& f.getFileName().toString().endsWith(".xml")).collect(Collectors.toList());
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		}

		var elzaFindingAidXml = xmls.stream().filter(p -> p.getFileName().toString().startsWith(FINDING_AID_DASH)
				&& p.getFileName().toString().endsWith(".xml")).findFirst();

		if (elzaFindingAidXml.isEmpty()) {
			log.warn("Directory not contail pevafund- file. Directory {}", path);
			return ImportResult.FAIL;
		}

		var findingaidXmlPath = elzaFindingAidXml.get();
		var fileName = findingaidXmlPath.getFileName().toString();
		var tmp = fileName.substring(FINDING_AID_DASH.length());
		var findingAidCode = tmp.substring(0, tmp.length() - ".xml".length());

		if (transactionTemplate.execute(t -> processFindingAid(path, findingAidCode, elzaFindingAidXml.get()))) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.FAIL;
		}
	}
	
	private boolean processFindingAid(Path dir, String findingAidCode, Path findingAidXmlPath) {
		
		var protocol = new ImportProtocol(dir);
        protocol.add("Zahájení importu");
		
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
			if (attachments != null) {
				attachments = attachments.stream().map(p -> dataDir.resolve(p.getFileName()))
						.collect(Collectors.toList());
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		if (findingAid == null) {
			findingAidService.createFindingAid(findingAidCode, Collections.singletonList(fund), institution, dataDir,
					dir, builder, attachments, false, true);
		} else {
			findingAidService.updateFindingAid(findingAid, dataDir, dir, builder, attachments, false, true);
		}
		return true;
	}
	
	private List<Path> readAttachments(Path dir, String findingaidCode, ApuSourceBuilder builder) {
		// get root apu
		var apu = builder.getMainApu();

		List<Path> attachments = new ArrayList<>();

		var filePdf = this.getPdfPath(dir, findingaidCode);
		if (Files.exists(filePdf)) {
			String name = "Archivní pomůcka - " + findingaidCode + ".pdf";

			var att = builder.addAttachment(apu, name, "application/pdf");

			attachments.add(filePdf);
		}
		return attachments;
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
	public Result reimport(ApuSource apuSource, FindingAid findingAid, Path apuPath) {
		
		if (!apuPath.getFileName().toString().startsWith(FINDING_AID_DASH)) {
			return Result.UNSUPPORTED;
		}

        var protocol = new ImportProtocol(apuPath);
        protocol.add("Zahájení reimportu");
        String fileXml = FINDING_AID_DASH + findingAid.getCode() + ".xml";

        ApuSourceBuilder builder;
        var ifai = new ImportFindingAidInfo(findingAid.getCode());
        try {
            builder = ifai.importFindingAidInfo(apuPath.resolve(fileXml), findingAid.getUuid(), databaseDataProvider);
            builder.setUuid(apuSource.getUuid());
            try (var os = Files.newOutputStream(apuPath.resolve("apusrc.xml"))) {
                builder.build(os, new ApuValidator(configurationLoader.getConfig()));
            }            
            var attachments = readAttachments(apuPath, findingAid.getCode(), builder);
            updateAttachments(findingAid, builder, attachments);            
        } catch (Exception e) {
            log.error("Fail to process downloaded {}, dir={}", fileXml, apuPath, e);
            protocol.add("Chyba " + e.getMessage());
            return Result.FAILED;
        }

        protocol.add("Reimport byl úspěšně dokončen");
        return Result.REIMPORTED;
	}
	
	private void updateAttachments(FindingAid findingAid, ApuSourceBuilder builder, List<Path> attachments) {
		var dbAttachments = attachmentRepository.findByApuSource(findingAid.getApuSource());
		if (dbAttachments.size() > 0) {
			// drop old attachment
			attachmentRepository.deleteInBatch(dbAttachments);
		}

		if (CollectionUtils.isNotEmpty(attachments)) {
			var mainApu = builder.getMainApu();
			Validate.isTrue(attachments.size() == mainApu.getAttchs().size(),
					"Attachment size does not match, attachments: %i, xml: %i", attachments.size(),
					mainApu.getAttchs().size());

			var attIt = mainApu.getAttchs().iterator();
			var fileIt = attachments.iterator();
			while (attIt.hasNext() && fileIt.hasNext()) {
				var att = attIt.next();
				var attPath = fileIt.next();

				Validate.notNull(att.getFile(), "DaoFile is null");
				Validate.notNull(att.getFile().getUuid(), "DaoFile without UUID");

				createAttachment(findingAid.getApuSource(), attPath.getFileName().toString(),
						UUID.fromString(att.getFile().getUuid()));
			}
		}
	}
	
	private void createAttachment(ApuSource apuSource, String fileName, UUID uuid) {
		var attachment = new Attachment();
		attachment.setApuSource(apuSource);
		attachment.setFileName(fileName);
		attachment.setUuid(uuid);
		attachment = attachmentRepository.save(attachment);
	}

}
