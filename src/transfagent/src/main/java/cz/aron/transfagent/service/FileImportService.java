package cz.aron.transfagent.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuSource;
import cz.aron.transfagent.domain.SourceType;
import cz.aron.transfagent.repository.ApuSourceRepository;

@Service
public class FileImportService implements SmartLifecycle {

    static final Logger log = LoggerFactory.getLogger(FileImportService.class);

    @Autowired
    ApuSourceRepository apuSourceRepository;

    private enum ThreadStatus {
        RUNNING, STOP_REQUEST, STOPPED
    }

    private ThreadStatus status;

    @Value("${aron.inputFolder}")
    private String inputFolder;

    /**
     * Monitorování vstupního adresáře
     * 
     * @throws IOException
     */
    private void importFile() throws IOException {
        // kontrola, zda vstupní adresář existuje
        if (Files.notExists(Path.of(inputFolder))) {
            log.error("Input folder {} not exists.", inputFolder);
            throw new RuntimeException("Input folder not exists.");
        }

        Path input = Path.of(inputFolder).resolve("input");
        Path processed = Path.of(inputFolder).resolve("processed");
        Path error = Path.of(inputFolder).resolve("error");

        Path direct = input.resolve("direct");

        // kontrola, zda adresář direct existuje
        if (Files.notExists(direct)) {
            log.error("Direct folder in input folder {} not exists.", inputFolder);
            throw new RuntimeException("Direct folder in input folder not exists.");
        }

        // zpracování souborů v adresáři direct
        processDirectFolder(direct, error);
        moveFolderTo(direct, processed.resolve("direct"));
    }

    /**
     * Zpracování souborů v adresáři direct
     * 
     * @param path adresář direct
     * @return true nebo false
     */
    private void processDirectFolder(Path path, Path error) throws IOException {
        File[] files = path.toFile().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                // process apux.xml or apux-XY.xml files
                if (name.startsWith("apux") && name.endsWith("xml")) {
                    return true;
                }
                return false;
            }
        });

        if (files.length == 0 || files.length > 1) {
            moveFolderTo(path, error);
            throw new RuntimeException("The folder should contain only one apux...xml file");
        }

        byte[] xml = Files.readAllBytes(files[0].toPath());
        try {
            ApuSource apux = unmarshalApuSourceFromXml(xml);
            List<Apu> apuList = apux.getApus().getApu();
            Apu apuItem = apuList.get(0);
            cz.aron.transfagent.domain.ApuSource apuSource = new cz.aron.transfagent.domain.ApuSource();
            apuSource.setData(xml);
            apuSource.setSourceType(SourceType.valueOf(apuItem.getType().toString()));
            apuSource.setUuid(UUID.fromString(apuItem.getUuid()));
            apuSource.setDeleted(false);
            apuSource.setDateImported(ZonedDateTime.now());
            apuSourceRepository.save(apuSource);
        } catch (JAXBException e) {
            moveFolderTo(path, error);
            throw new RuntimeException("Failed to parse", e);
        }
    }

    /**
     * Vytváření objektů na základě XML souboru
     * 
     * @param xml
     * @return cz.aron.apux._2020.ApuSource
     * @throws JAXBException
     * @throws IOException
     */
    private ApuSource unmarshalApuSourceFromXml(byte[] xml) throws JAXBException, IOException {
        ApuSource apuSource = null;
        try (InputStream is = new ByteArrayInputStream(xml)) {
            Unmarshaller unmarshaller = ApuSourceBuilder.apuxXmlContext.createUnmarshaller();
            unmarshaller.setSchema(ApuSourceBuilder.schemaApux);
            apuSource = ((JAXBElement<ApuSource>) unmarshaller.unmarshal(is)).getValue();
        }
        return apuSource;
    }

    /**
     * Přesunutí adresáře do jiného adresáře
     * 
     * @param source
     * @param target
     * @throws IOException
     */
    private void moveFolderTo(Path source, Path target) throws IOException {
        if (Files.exists(target)) {
            String dateTime = new SimpleDateFormat("_yyyy_MM_dd_HH_mm_ss").format(new Date());
            target = Path.of(target.toString() + dateTime);
        }
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        Files.createDirectories(source);
    }

    public void run() {
        while (status == ThreadStatus.RUNNING) {
            try {
                importFile();
            } catch (Exception e) {
                log.error("Error in import file. ", e);
            }
        }
        status = ThreadStatus.STOPPED;
    }

    @Override
    public void start() {
        status = ThreadStatus.RUNNING;
        new Thread(() -> {
            run();
        }).start();
    }

    @Override
    public void stop() {
        status = ThreadStatus.STOP_REQUEST;
    }

    @Override
    public boolean isRunning() {
        return status == ThreadStatus.RUNNING;
    }

    public static JAXBContext createJaxbContext(Class<?>... classesToBeBound) {
        try {
            return JAXBContext.newInstance(classesToBeBound);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
