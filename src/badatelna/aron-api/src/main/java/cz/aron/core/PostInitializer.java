package cz.aron.core;

import cz.aron.core.integration.ApuProcessor;
import cz.aron.core.model.ApuEntity;
import cz.aron.core.model.ApuSource;
import cz.aron.core.model.ApuSourceRepository;
import cz.aron.core.model.ApuStore;
import cz.aron.core.model.types.TypesHolder;
import cz.aron.core.util.SimpleProfiler;
import cz.inqool.eas.common.domain.index.reindex.ReindexService;
import cz.inqool.eas.common.storage.file.FileManager;
import cz.inqool.eas.common.storage.file.FileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Component
@Slf4j
public class PostInitializer implements ApplicationListener<ApplicationReadyEvent> {

    @Inject private ApuProcessor apuProcessor;
    @Inject private ResourceLoader resourceLoader;
    @Inject private ApuSourceRepository apuSourceRepository;
    @Inject private ApuStore apuStore;
    @Inject private ReindexService reindexService;
    @Inject private FileManager fileManager;
    @Inject private FileStore fileStore;
    @Inject private TypesHolder typesHolder;

    @Value("${environment}")
    private String environment;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Running PostInitializer");
        try {
//            wipe();
            if (apuSourceRepository.countAll() > 0) {
                log.info("Data already exists in db, not loading samples...");
                checkCrcOrReindex(typesHolder.getCurrentConfigCrc());
            }
            else {
                reindexService.reindex(null);
                log.info("Loading example data...");
                if (environment.startsWith("docker")) {                    
                    apuProcessor.processTestingInputStream(Path.of("./init-data/examples/institutions.xml"));                    
                    log.info("next file...");
                    apuProcessor.processTestingInputStream(Path.of("./init-data/examples/institutionEntities.xml"));
                    log.info("next file...");
                    apuProcessor.processTestingInputStream(Path.of("./init-data/examples/archdesc-1820.xml"));
                    log.info("next file...");
                    apuProcessor.processTestingInputStream(Path.of("./init-data/examples/fund-1820.xml"));
                }
                else if (Files.exists(Path.of("./init-data/sample"))) {
                    Files.list(Path.of("./init-data/sample")).forEach(
                            path -> {
                                log.info(" - " + path.getFileName().toString() + "...");
                                try {
                                    apuProcessor.processTestingInputStream(path);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );
                }
                SimpleProfiler.dump();
            }
        } catch (Exception e) {
            log.error("boo boo", e);
        }
        log.info("PostInitializer complete");
    }

    private void checkCrcOrReindex(Long currentCrc) {
        try {
            Path crcPath = Path.of("./lastConfigCrc.txt");
            Long previousCrc = null;
            if (Files.exists(crcPath)) {
                previousCrc = Long.valueOf(Files.readString(crcPath));
            }
            if (!currentCrc.equals(previousCrc)) {
                reindexService.reindex(null);
                Files.writeString(crcPath, String.valueOf(currentCrc));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void wipe() {
        for (ApuEntity apuEntity : apuStore.listAll()) {
            apuStore.delete(apuEntity.getId());
        }
        for (ApuSource apuSource : apuSourceRepository.listAll()) {
            apuSourceRepository.delete(apuSource.getId());
        }
        reindexService.reindex(null);
    }
}
