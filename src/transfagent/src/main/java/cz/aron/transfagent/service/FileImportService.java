package cz.aron.transfagent.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.domain.ApuSource;
import cz.aron.transfagent.repository.ApuSourceRepository;

@Service
public class FileImportService implements SmartLifecycle {

    static final Logger log = LoggerFactory.getLogger(FileImportService.class);

    private static final JAXBContext JAXB_CONTEXT = createJaxbContext(ApuSource.class);

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
        if (processDirectFolder(direct)) {
            moveFolderTo(direct, processed.resolve("direct"));
            Files.createDirectories(input.resolve("direct"));
        }
    }

    /**
     * Zpracování souborů v adresáři direct
     * 
     * @param path adresář direct
     * @return true nebo false
     */
    private boolean processDirectFolder(Path path) {
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
        for (File file : files) {
            ApuSource apuSource = createApuSourceFromXml(file);
            apuSourceRepository.save(apuSource);
        }
        return files.length > 0;
    }

    /**
     * Vytváření objektů na základě XML souboru
     * 
     * @param file
     * @return
     */
    private ApuSource createApuSourceFromXml(File file) {
        ApuSource apuSource = null;
        try (InputStream is = new FileInputStream(file)) {
            apuSource = JAXB_CONTEXT.createUnmarshaller().unmarshal(new StreamSource(is), ApuSource.class).getValue();
        } catch (JAXBException | IOException e) {
            throw new RuntimeException("Failed to parse", e);
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
