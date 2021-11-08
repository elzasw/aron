package cz.aron.transfagent.peva;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.peva.ImportPevaFundInfo.FundProvider;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.ArchivalEntityService;
import cz.aron.transfagent.service.AttachmentService;
import cz.aron.transfagent.service.FundService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFundService.FundImporter;
import cz.aron.transfagent.service.importfromdir.ReimportProcessor;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class ImportPevaFund implements FundImporter, FundProvider {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaFund.class);
	
	private static final String PREFIX = "pevafund-";
	
	private static final String FUND_SOURCE = "peva";
	
    private final FundRepository fundRepository;
    
    private final InstitutionRepository institutionRepository;

    private final DatabaseDataProvider databaseDataProvider;

    private final ConfigurationLoader configurationLoader;
    
    private final FundService fundService;
    
    private final StorageService storageService;

    private final ConfigPeva2 configPeva2;
    
    private final TransactionTemplate tt;
    
    private final Peva2CodeListProvider codeListProvider;
    
    private final Peva2CachedEntityDownloader entityDownloader;
    
    private final ArchivalEntityService archivalEntityService;
    
    private final AttachmentService attachmentService;
    
	public ImportPevaFund(FundRepository fundRepository, InstitutionRepository institutionRepository,
			DatabaseDataProvider databaseDataProvider, ConfigurationLoader configurationLoader, ConfigPeva2 configPeva2,
			FundService fundService, StorageService storageService, TransactionTemplate tt,
			Peva2CodeListDownloader codeListDownloader, Peva2CachedEntityDownloader entityDownloader,
			ArchivalEntityService archivalEntityService, AttachmentService attachmentService) {
		super();
		this.fundRepository = fundRepository;
		this.institutionRepository = institutionRepository;
		this.databaseDataProvider = databaseDataProvider;
		this.configurationLoader = configurationLoader;
		this.configPeva2 = configPeva2;
		this.fundService = fundService;
		this.storageService = storageService;
		this.tt = tt;
		this.codeListProvider = new Peva2CodeListProvider(codeListDownloader);
		this.entityDownloader = entityDownloader;
		this.archivalEntityService = archivalEntityService;
		this.attachmentService = attachmentService;
	}
    
	@Override
	public ImportResult processPath(Path path) {
		
		if (!path.getFileName().toString().startsWith(PREFIX)) {
			return ImportResult.UNSUPPORTED;
		}

		var pevaFundXml = getPevaFundXmlPath(path);
		
		if (pevaFundXml.isEmpty()) {
			log.warn("Directory not contail pevafund- file. Directory {}", path);
            return ImportResult.FAIL;
		}
		
		if (processFundPeva(path,pevaFundXml.get())) {
			return ImportResult.IMPORTED;
		} else {
			return ImportResult.UNSUPPORTED;	
		}
	}
	
	
	private static Optional<Path> getPevaFundXmlPath(Path path) {
		List<Path> xmls;
		try (var stream = Files.list(path)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) &&  f.getFileName().toString().startsWith(PREFIX)
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
        	log.error("Fail to find peva fund xml path={}",path);
            throw new UncheckedIOException(ioEx);
        }
		
		return xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();	
	}
	

	private boolean processFundPeva(Path dir, Path fundXml) {
        var fileName = fundXml.getFileName().toString();
        var tmp = fileName.substring(PREFIX.length());
        var fundCode = tmp.substring(0, tmp.length() - ".xml".length());
        
        var fund = fundRepository.findByCode(fundCode);
        UUID fundUuid = (fund!=null)?fund.getUuid():null;

        var ifi = new ImportPevaFundInfo(configPeva2.getFundProperties());
        ApuSourceBuilder apusrcBuilder;
 
        try {
            apusrcBuilder = ifi.importFundInfo(fundXml, fundUuid, databaseDataProvider, this, codeListProvider, entityDownloader);
        } catch (IOException e) {
        	log.error("Fail to import fund, path={}",fundXml,e);
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
        	log.error("Fail to import fund, fail to parse data, path={}",fundXml,e);
            throw new IllegalStateException(e);
        }
        
		var attachments = new ArrayList<Path>();
		if (fund != null) {
			apusrcBuilder.setUuid(fund.getApuSource().getUuid());
			tt.executeWithoutResult(t -> {
				fundRepository.findById(fund.getId()).ifPresentOrElse(f -> {
					attachments.addAll(readAllAttachments(dir, f, apusrcBuilder));
				}, () -> {
					throw new IllegalStateException("Fund not exist id="+fund.getId());
				});

			});
		}

        var institutionCode = ifi.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
			log.error("Fail to import fund, institution not exist, path={}, institution={}", fundXml, institutionCode);
            throw new IllegalStateException("The entry Institution code={" + institutionCode + "} must exist.");
        }

        try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
            apusrcBuilder.build(fos, new ApuValidator(configurationLoader.getConfig()));
        } catch (IOException ioEx) {
        	log.error("Fail to import fund, path={}", fundXml,ioEx);
            throw new UncheckedIOException(ioEx);
        } catch (JAXBException e) {
        	log.error("Fail to import fund, path={}", fundXml,e);
            throw new IllegalStateException(e);
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
        } catch (IOException e) {
        	log.error("Fail to import fund, fail to move to directory={}",dir,e);
            throw new IllegalStateException(e);
        }                
        
        tt.executeWithoutResult(t->{        	
        	var fnd = fund;        	
        	if (fnd == null) {
                fnd = fundService.createFund(institution, dataDir, dir, apusrcBuilder, fundCode, FUND_SOURCE);            
            } else {
                fundService.updateFund(fnd, dataDir, dir, true);
            }
            var uuids = ifi.getOriginatorIds().stream().map(id->UUID.fromString(id)).collect(Collectors.toList());
            archivalEntityService.registerAccessibleEntities(uuids, ImportPevaOriginator.ENTITY_CLASS, fnd.getApuSource());
            attachmentService.updateAttachments(fnd.getApuSource(), apusrcBuilder, attachments);
        });
        return true;
    }

	public static NadPrimarySheet readPevaFundFromDir(Path directory) throws IOException, JAXBException {
						
		var pevaFundXml = getPevaFundXmlPath(directory);
		
		if (pevaFundXml.isEmpty()) {
			log.warn("Directory not contail pevafund- file. Directory {}", directory);
            throw new FileNotFoundException("pevafund-*.xml");
		}
		
		GetNadSheetResponse gnsr = Peva2XmlReader.unmarshalGetNadSheetResponse(pevaFundXml.get());
		return gnsr.getNadPrimarySheet();	
	}

	@Override
	public NadPrimarySheet getFundByUUID(UUID uuid) throws IOException, JAXBException {		
		var path = tt.execute(t->{
			var fund = fundRepository.findByUuid(uuid);
			return storageService.getDataPath().resolve(fund.getApuSource().getDataDir());				
		});
		return readPevaFundFromDir(path);
	}
	

	@Override
	public ReimportProcessor.Result reimport(ApuSource apuSource, Fund fund, Path apuPath) {
		
		if (!apuPath.getFileName().toString().startsWith(PREFIX)) {
			return ReimportProcessor.Result.UNSUPPORTED;
		}
		
		var fileName = PREFIX + fund.getCode() + ".xml";
		var apuDir = storageService.getApuDataDir(apuSource.getDataDir());
		ApuSourceBuilder apuSourceBuilder;
		var ifi = new ImportPevaFundInfo(configPeva2.getFundProperties());
		try {
			apuSourceBuilder = ifi.importFundInfo(apuDir.resolve(fileName), fund.getUuid(), databaseDataProvider, this,
					codeListProvider, entityDownloader);
			apuSourceBuilder.setUuid(apuSource.getUuid());
			var attachments = new ArrayList<Path>();
			tt.executeWithoutResult(t -> {
				fundRepository.findById(fund.getId()).ifPresentOrElse(f -> {
					attachments.addAll(readAllAttachments(apuSource, f, apuSourceBuilder));
				}, () -> {
					throw new IllegalStateException("Fund not exist id="+fund.getId());
				});

			});						
			try (var os = Files.newOutputStream(apuDir.resolve("apusrc.xml"))) {
				apuSourceBuilder.build(os, new ApuValidator(configurationLoader.getConfig()));
			}
			tt.executeWithoutResult(t -> {
				// reload
				fundRepository.findById(fund.getId()).ifPresentOrElse(f -> {
					fundService.updateFund(f, apuPath, apuPath, false);
					var uuids = ifi.getOriginatorIds().stream().map(id -> UUID.fromString(id))
							.collect(Collectors.toList());
					archivalEntityService.registerAccessibleEntities(uuids, ImportPevaOriginator.ENTITY_CLASS,
							fund.getApuSource());
					attachmentService.updateAttachments(apuSource, apuSourceBuilder, attachments);
				}, () -> {
					log.error("Fund not exist id={}",fund.getId());
					throw new IllegalStateException();
				});
			});
		} catch (Exception e) {
			log.error("Fail to process downloaded {}, dir={}", fileName, apuDir, e);
			return ReimportProcessor.Result.FAILED;
		}
		log.info("Fund id={}, uuid={} reimported", fund.getId(), fund.getUuid());
		return ReimportProcessor.Result.REIMPORTED;
	}	
	
	private List<Path> readAllAttachments(ApuSource apuSource, Fund fund, ApuSourceBuilder apuSourceBuilder) {
		var ret = new ArrayList<Path>();
		var fundProp = configPeva2.getFundProperties();
		if (fundProp != null && fundProp.isAggregateAttachments()&&fund!=null) {
			// copy attachments
			try {
				var attachments = attachmentService.copyAttachmentsFromApus(apuSource, fund.getFindingAids().stream()
						.map(fa -> fa.getApuSource()).collect(Collectors.toList()));
				for(var attachment:attachments) {
					String mimetype = null;
					try {
						mimetype = this.storageService.detectMimetype(attachment);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					apuSourceBuilder.addAttachment(apuSourceBuilder.getMainApu(), attachment.getFileName().toString(), mimetype);
					ret.add(attachment);
				}				
			} catch (IOException e) {
				log.error("Fund, fail to import funding aid attachment, apuSource={}",apuSource.getUuid(),e);
				throw new UncheckedIOException(e);
			}
		}
		return ret;
	}
	
	
	private List<Path> readAllAttachments(Path targetDir, Fund fund, ApuSourceBuilder apuSourceBuilder) {
		var ret = new ArrayList<Path>();
		var fundProp = configPeva2.getFundProperties();
		if (fundProp != null && fundProp.isAggregateAttachments() && fund != null) {
			// copy attachments
			try {
				var attachments = attachmentService.copyAttachmentsFromApus(targetDir,
						fund.getFindingAids().stream().map(fa -> fa.getApuSource()).collect(Collectors.toList()));
				for (var attachment : attachments) {
					String mimetype = null;
					try {
						mimetype = this.storageService.detectMimetype(attachment);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					apuSourceBuilder.addAttachment(apuSourceBuilder.getMainApu(), attachment.getFileName().toString(),
							mimetype);
					ret.add(attachment);
				}
			} catch (IOException e) {
				log.error("Fund, fail to import funding aid attachment, fund={}",fund.getUuid(),e);
				throw new UncheckedIOException(e);
			}
		}
		return ret;
	}

}
