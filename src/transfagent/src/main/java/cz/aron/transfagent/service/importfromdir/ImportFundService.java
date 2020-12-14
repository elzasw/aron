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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    StorageService storageService;

    @Autowired
    FundRepository fundRepository;

    @Autowired
    ApuSourceRepository apuSourceRepository;

    @Autowired
    InstitutionRepository institutionRepository;

    @Autowired
    CoreQueueRepository coreQueueRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

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
            apusrcBuilder = ifi.importFundInfo(fundXml.get(), new DatabaseDataProvider());
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

        Path dataDir;
        try {
            dataDir = storageService.moveToDataDir(dir);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (fund == null) {
            createFund(dataDir, dir, apusrcBuilder, fundCode);
        } else {
            updateFund(fund, dataDir, dir, apusrcBuilder, fundCode);
        }
        return true;
	}

    private void createFund(Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, String fundCode) {

        var institutionUuid = UUID.randomUUID();
        var fundUuid = UUID.randomUUID();

        transactionTemplate.execute(t -> {
            var apuSource = new ApuSource();
            apuSource.setOrigDir(origDir.getFileName().toString());
            apuSource.setDataDir(dataDir.toString());
            apuSource.setSourceType(SourceType.FUND);
            apuSource.setUuid(UUID.fromString(apusrcBuilder.getApusrc().getUuid()));
            apuSource.setDeleted(false);
            apuSource.setDateImported(ZonedDateTime.now());
            apuSource = apuSourceRepository.save(apuSource);

            var institution = new Institution();
            institution.setCode(fundCode); // TODO upřesnit
            institution.setSource("source");
            institution.setUuid(institutionUuid);
            institution.setApuSource(apuSource);
            institution = institutionRepository.save(institution);

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

    private void updateFund(Fund fund, Path dataDir, Path origDir, ApuSourceBuilder apusrcBuilder, String fundCode) {

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
        log.info("Fund updated code={}, uuid={}, original data dir {}", fundCode, fund.getUuid(), oldDir);
    }

}
