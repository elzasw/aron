package cz.aron.transfagent.service.importfromdir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.domain.CoreQueue;
import cz.aron.transfagent.domain.Fund;
import cz.aron.transfagent.domain.Institution;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.elza.ImportFundInfo;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.CoreQueueRepository;
import cz.aron.transfagent.repository.FundRepository;
import cz.aron.transfagent.repository.InstitutionRepository;
import cz.aron.transfagent.service.StorageService;
import cz.aron.transfagent.transformation.DatabaseDataProvider;

@Service
public class ImportFundService {

    private static final Logger log = LoggerFactory.getLogger(ImportFundService.class);

    private final StorageService storageService;

    private final FundRepository fundRepository;

    private final ApuSourceRepository apuSourceRepository;

    private final InstitutionRepository institutionRepository;

    private final CoreQueueRepository coreQueueRepository;

    private final TransactionTemplate transactionTemplate;

    public ImportFundService(StorageService storageService, FundRepository fundRepository,
                             ApuSourceRepository apuSourceRepository, InstitutionRepository institutionRepository,
                             CoreQueueRepository coreQueueRepository, TransactionTemplate transactionTemplate) {
        this.storageService = storageService;
        this.fundRepository = fundRepository;
        this.apuSourceRepository = apuSourceRepository;
        this.institutionRepository = institutionRepository;
        this.coreQueueRepository = coreQueueRepository;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Zpracování adresářů s archivními soubory
     * 
     * @param dir zpracovavany adresar
     */
    public boolean processDirectory(Path dir) {

        List<Path> xmls;
        try (var stream = Files.list(dir)) {
            xmls = stream
                    .filter(f -> Files.isRegularFile(f) && f.getFileName().toString().startsWith("fund")
                            && f.getFileName().toString().endsWith(".xml"))
                    .collect(Collectors.toList());
        } catch (IOException ioEx) {
            throw new UncheckedIOException(ioEx);
        }

        var fundXml = xmls.stream()
                .filter(p -> p.getFileName().toString().startsWith("fund-")
                        && p.getFileName().toString().endsWith(".xml"))
                .findFirst();

        if (fundXml.isEmpty()) {
            log.warn("Directory is empty {}", dir);
            return false;
        }

        var fileName = fundXml.get().getFileName().toString();
        var tmp = fileName.substring("fund-".length());
        var fundCode = tmp.substring(0, tmp.length() - ".xml".length());

        var ifi = new ImportFundInfo();
        ApuSourceBuilder apusrcBuilder;

        try {
            apusrcBuilder = ifi.importFundInfo(fundXml.get(), new DatabaseDataProvider(institutionRepository));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JAXBException e) {
        	throw new IllegalStateException(e);
        }

        var fund = fundRepository.findByCode(fundCode);
        if (fund != null) {
            apusrcBuilder.getApusrc().setUuid(fund.getApuSource().getUuid().toString());
            apusrcBuilder.getApusrc().getApus().getApu().get(0).setUuid(fund.getUuid().toString());
        }

        var institutionCode = ifi.getInstitutionCode();
        var institution = institutionRepository.findByCode(institutionCode);
        if (institution == null) {
        	throw new NullPointerException("The entry institution code={" + institutionCode + "} must exist.");
        }

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (fund == null) {
            createFund(institution, dataDir, dir, apusrcBuilder, fundCode);
        } else {
            updateFund(fund, dataDir, dir);
        }
        return true;
	}

    private void createFund(Institution institution, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, String fundCode) {

        var fundUuid = UUID.randomUUID();
        var apuSourceUuidStr = apusrcBuilder.getApusrc().getUuid();
        var apuSourceUuid = apuSourceUuidStr == null? UUID.randomUUID() : UUID.fromString(apuSourceUuidStr); 

        transactionTemplate.execute(t -> {
            var apuSource = new ApuSource();
            apuSource.setOrigDir(origDir.getFileName().toString());
            apuSource.setDataDir(dataDir.toString());
            apuSource.setSourceType(SourceType.FUND);
            apuSource.setUuid(apuSourceUuid);
            apuSource.setDeleted(false);
            apuSource.setDateImported(ZonedDateTime.now());
            apuSource = apuSourceRepository.save(apuSource);

            var fund = new Fund();
            fund.setApuSource(apuSource);
            fund.setInstitution(institution);
            fund.setCode(fundCode);
            fund.setSource("source");
            fund.setUuid(fundUuid);
            fund = fundRepository.save(fund);

            var coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("Fund created code={}, uuid={}", fundCode, fundUuid);
    }

    private void updateFund(Fund fund, Path dataDir, Path origDir) {

        var oldDir = fund.getApuSource().getDataDir();

        transactionTemplate.execute(t -> {
            var apuSource = fund.getApuSource();
            apuSource.setDataDir(dataDir.toString());
            apuSource.setOrigDir(origDir.getFileName().toString());

            var coreQueue = new CoreQueue();
            coreQueue.setApuSource(apuSource);

            apuSourceRepository.save(apuSource);
            coreQueueRepository.save(coreQueue);
            return null;
        });
        log.info("Fund updated code={}, uuid={}, original data dir {}", fund.getCode(), fund.getUuid(), oldDir);
    }

}
