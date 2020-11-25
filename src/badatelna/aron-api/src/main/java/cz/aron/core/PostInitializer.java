package cz.aron.core;

import cz.aron.core.image.ImageProcessor;
import cz.aron.core.integration.ApuProcessor;
import cz.aron.core.model.ApuEntity;
import cz.aron.core.model.ApuSource;
import cz.aron.core.model.ApuSourceRepository;
import cz.aron.core.model.ApuStore;
import cz.inqool.eas.common.domain.index.reindex.ReindexService;
import cz.inqool.eas.common.storage.file.File;
import cz.inqool.eas.common.storage.file.FileManager;
import cz.inqool.eas.common.storage.file.FileStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;


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
    @Inject private ImageProcessor imageProcessor;
    @Inject private FileManager fileManager;
    @Inject private FileStore fileStore;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Running PostInitializer");
        try {
//            wipe();
            loadSampleImage();
            if (apuSourceRepository.countAll() > 0) {
                log.info("Data already exists in db, not loading samples...");
            }
            else {
                try (InputStream inputStream = resourceLoader.getResource("classpath:/examples/apux-00.xml").getInputStream()) {
                    apuProcessor.processTestingInputStream(inputStream);
                }
                try (InputStream inputStream = resourceLoader.getResource("classpath:/examples/institutions.xml").getInputStream()) {
                    apuProcessor.processTestingInputStream(inputStream);
                }
                try (InputStream inputStream = resourceLoader.getResource("classpath:/examples/institutionEntities.xml").getInputStream()) {
                    apuProcessor.processTestingInputStream(inputStream);
                }
                try (InputStream inputStream = resourceLoader.getResource("classpath:/examples/archdesc-1820.xml").getInputStream()) {
                    apuProcessor.processTestingInputStream(inputStream);
                }
            }
        } catch (Exception e) {
            log.error("boo boo", e);
        }
        log.info("PostInitializer complete");
    }

    private void loadSampleImage() throws IOException {
        if (fileStore.countAll() == 0) {
            Resource resource = resourceLoader.getResource("classpath:/cell.jpg");
            try (InputStream is = resource.getInputStream()) {
                File imageFile = fileManager.store("cell.jpg", 3244914, MimeTypeUtils.IMAGE_JPEG_VALUE, is);
                imageProcessor.process(imageFile.getId());
                log.info("Created tile testing image with id " + imageFile.getId());
            }
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
