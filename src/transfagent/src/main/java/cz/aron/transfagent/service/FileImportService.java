package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

@Service
public class FileImportService implements SmartLifecycle {

    static final Logger log = LoggerFactory.getLogger(FileImportService.class);

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
        } else {
            moveFolderTo(direct, error);
        }
    }

    /**
     * Zpracování souborů v adresáři direct
     * 
     * @param path
     * @return
     */
    private boolean processDirectFolder(Path path) {
        // TODO process apux.xml or apux-XY.xml files
        return true;
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

}
