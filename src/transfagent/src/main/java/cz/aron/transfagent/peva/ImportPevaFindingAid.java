package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux.ApuValidator;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.FindingAidCopy;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.repository.FindingAidRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.FindingAidService;
import cz.aron.transfagent.service.ImportProtocol;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFindingAidService.FindingAidImporter;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class ImportPevaFindingAid implements FindingAidImporter {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaFindingAid.class);
	
	private final String PEVA_FINDING_AID = "pevafa";
	   
	private final String PEVA_FINDING_AID_DASH = PEVA_FINDING_AID + "-";
	
	private final StorageService storageService;
	
	private final FindingAidService findingAidService;
	
	private final FindingAidRepository findingAidRepository;
	
	private final FundRepository fundRepository;
	
	private final InstitutionRepository institutionRepository;
	
	private final DatabaseDataProvider databaseDataProvider;
	
	private final ConfigurationLoader configurationLoader;
	
	private final TransactionTemplate transactionTemplate;
	
	private final Peva2CodeListProvider codeListProvider;
	
	private final ConfigPeva2 configPeva2;
	
	public ImportPevaFindingAid(StorageService storageService, FindingAidService findingAidService,
			FindingAidRepository findingAidRepository, FundRepository fundRepository,
			InstitutionRepository institutionRepository, DatabaseDataProvider databaseDataProvider,
			ConfigurationLoader configurationLoader, TransactionTemplate transactionTemplate,
			Peva2CodeListProvider codeListProvider, ConfigPeva2 configPeva2) {
		super();
		this.storageService = storageService;
		this.findingAidService = findingAidService;
		this.findingAidRepository = findingAidRepository;
		this.fundRepository = fundRepository;
		this.institutionRepository = institutionRepository;
		this.databaseDataProvider = databaseDataProvider;
		this.configurationLoader = configurationLoader;
		this.transactionTemplate = transactionTemplate;
		this.codeListProvider = codeListProvider;
		this.configPeva2 = configPeva2;
	}

	@Override
	public ImportResult processPath(Path path) {

		if (!path.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)) {
			return ImportResult.UNSUPPORTED;
		}

		List<Path> xmls;
		try (var stream = Files.list(path)) {
			xmls = stream
					.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)
							&& f.getFileName().toString().endsWith(".xml"))
					.collect(Collectors.toList());
		} catch (IOException ioEx) {
			throw new UncheckedIOException(ioEx);
		}

		var pevaFindingAidXml = xmls.stream().filter(p -> p.getFileName().toString().startsWith(PEVA_FINDING_AID_DASH)
				&& p.getFileName().toString().endsWith(".xml")).findFirst();

		if (pevaFindingAidXml.isEmpty()) {
			log.warn("Directory not contail pevafund-* file. Directory {}", path);
			return ImportResult.FAIL;
		}

		var findingaidXmlPath = pevaFindingAidXml.get();
		var fileName = findingaidXmlPath.getFileName().toString();
		var tmp = fileName.substring(PEVA_FINDING_AID_DASH.length());
		var findingAidCode = tmp.substring(0, tmp.length() - ".xml".length());

		if (transactionTemplate.execute(t -> processFindingAidPeva(path, pevaFindingAidXml.get(), findingAidCode))) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.UNSUPPORTED;
		}
	}	
	
	private boolean processFindingAidPeva(Path dir, Path findingAidXml, String findingAidCode) {
		
		var protocol = new ImportProtocol(dir);
        protocol.add("Zahájení importu");
		
        var ifai = new ImportPevaFindingAidInfo(codeListProvider.getCodeLists(), configPeva2.getFindingAidProperties());
        ApuSourceBuilder builder;
        try {
            builder = ifai.importFindingAidInfo(findingAidXml, databaseDataProvider);
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

		var findingAidCopies = readFindingAidCopies(findingaidUuid.toString());
		if (!findingAidCopies.isEmpty()) {
			var apu = builder.getMainApu();
			Part partInfo = ApuSourceBuilder.getFirstPart(apu, CoreTypes.PT_FINDINGAID_INFO);			
			for (var findingAidCopy : findingAidCopies) {
				var institutionInfo = databaseDataProvider.getInstitutionApu(findingAidCopy.getInstitution().getExternalId());				
				ApuSourceBuilder.addString(partInfo, "FINDINGAID_COPY", institutionInfo.getName() + ": " + findingAidCopy.getEvidenceNumber());
			}
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
			if (attachments != null) {
				attachments = attachments.stream().map(p -> dataDir.resolve(p.getFileName()))
						.collect(Collectors.toList());
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		if (findingAid == null) {
			findingAidService.createFindingAid(findingAidCode, funds, institution, dataDir, dir, builder, attachments,
					configPeva2.getFundProperties().isAggregateAttachments());
		} else {
			findingAidService.updateFindingAid(findingAid, dataDir, dir, builder, attachments,
					configPeva2.getFundProperties().isAggregateAttachments());
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
						mimetype = this.storageService.detectMimetype(f);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					builder.addAttachment(apu, f.getFileName().toString(), mimetype);
					attachments.add(f);
				}
			});
		} catch (IOException e) {
			log.error("Fail to read attachments from directory {}", dir);
			throw new UncheckedIOException(e);
		}
		return attachments;
	}

	private List<FindingAidCopy> readFindingAidCopies(String findingAidId) {
		var ret = new ArrayList<FindingAidCopy>();
		var dir = storageService.getInputPath().resolve("facopies").resolve(findingAidId);
		if (Files.isDirectory(dir)) {
			try (var stream = Files.list(dir)) {
				stream.forEach(f -> {
					if (Files.isRegularFile(f)) {
						try {
							var facResp = Peva2XmlReader.unmarshalGetFindingAidCopyResponse(f);
							ret.add(facResp.getFindingAidCopy());
						} catch (IOException | JAXBException e) {
							log.error("Fail to deserialize finding aid copy {}", f, e);
							throw new IllegalStateException(e);
						}
					}
				});
			} catch (IOException e) {
				// ignore
				log.error("Fail to read directory containing finding aid copies {}", dir, e);
			}
		}
		return ret;
	}

}
