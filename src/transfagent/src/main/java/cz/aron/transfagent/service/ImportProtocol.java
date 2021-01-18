package cz.aron.transfagent.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

public class ImportProtocol {

    private final static Logger log = LoggerFactory.getLogger(ImportProtocol.class);  

    private final String DATE_TIME_PATTERN = "MM.dd.yyyy HH:mm:ss";

    private final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_TIME_PATTERN);

    private final String FILE_NAME = "protokol.txt";

    private Path logPath;

    public ImportProtocol(Path logPath) {
        this.logPath = logPath;
        init();
    }

    public void init() {
        try {
            Files.deleteIfExists(logPath.resolve(FILE_NAME));
            Files.createFile(logPath.resolve(FILE_NAME));
        } catch (IOException e) {
            log.error("Error creating file {}", logPath.resolve(FILE_NAME));
            throw new IllegalStateException("Error creating file " + logPath.resolve(FILE_NAME));
        }
    }

    public void setLogPath(Path logPath) {
        this.logPath = logPath;
    }

    public void add(String msg) {
        var today = Calendar.getInstance().getTime();
        var message = String.format("%s: %s\n", DATE_FORMAT.format(today), msg);
        try {
            Files.write(logPath.resolve(FILE_NAME), message.getBytes(), StandardOpenOption.APPEND);
        }
        catch (IOException e) {
            log.error("Error writing file {}", logPath.resolve(FILE_NAME));
            throw new IllegalStateException("Error writing file " + logPath.resolve(FILE_NAME));
        }
    }

}
