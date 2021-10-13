package cz.aron.transfagent.peva;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.transfagent.config.ConfigPeva2;
import cz.aron.transfagent.config.ConfigurationLoader;
import cz.aron.transfagent.peva.ImportPevaFundInfo.FundProvider;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.FundService;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.service.importfromdir.ImportFundService.FundImporter;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
@ConditionalOnProperty(value = "peva2.url")
public class ImportPevaFund implements FundImporter, FundProvider {
	
	private static final Logger log = LoggerFactory.getLogger(ImportPevaFund.class);
	
	private static final String PREFIX = "pevafund-";
	
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
    
    public ImportPevaFund(FundRepository fundRepository, InstitutionRepository institutionRepository,
			DatabaseDataProvider databaseDataProvider, ConfigurationLoader configurationLoader,
			ConfigPeva2 configPeva2, FundService fundService, StorageService storageService, TransactionTemplate tt, Peva2CodeListDownloader codeListDownloader,
			Peva2CachedEntityDownloader entityDownloader) {
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
	}
    
	@Override
	public ImportResult processPath(Path path) {
		
		if (!path.getFileName().toString().startsWith(PREFIX)) {
			return ImportResult.UNSUPPORTED;
		}
		
		List<Path> xmls;
		try (var stream = Files.list(path)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) &&  f.getFileName().toString().startsWith(PREFIX)
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
		
		var pevaFundXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();
		
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
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
        
        if (fund != null) {
            apusrcBuilder.setUuid(fund.getApuSource().getUuid());
        }

        var institutionCode = ifi.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
            throw new IllegalStateException("The entry Institution code={" + institutionCode + "} must exist.");
        }

        try (var fos = Files.newOutputStream(dir.resolve("apusrc.xml"))) {
            apusrcBuilder.build(fos, new ApuValidator(configurationLoader.getConfig()));
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (fund == null) {
            fundService.createFund(institution, dataDir, dir, apusrcBuilder, fundCode);
        } else {
            fundService.updateFund(fund, dataDir, dir);
        }
        return true;    	
    }

	public static NadPrimarySheet readPevaFundFromDir(Path directory) throws IOException, JAXBException {
		
		List<Path> xmls;
		try (var stream = Files.list(directory)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) &&  f.getFileName().toString().startsWith(PREFIX)
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }
		
		var pevaFundXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();
		
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
		

}
