package cz.aron.transfagent.service;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import cz.aron.transfagent.config.ConfigLevelEnrichment;

@ConditionalOnProperty(value="levelEnrichment.levelUrls")
@Service
public class LevelEnrichmentService {
    
    private static final Logger log = LoggerFactory.getLogger(LevelEnrichmentService.class);
    
    private final String prefix;
    
    private final String label;
    
    private final Path mappingPath;
    
    private Map<String,String> mapping = new HashMap<>();
    
    private FileTime lastModification = null;
    
    private long lastCheck = 0;
    
    public LevelEnrichmentService(ConfigLevelEnrichment configLevelEnrichment) {
        prefix = configLevelEnrichment.getLevelUrlsPrefix();
        mappingPath = Paths.get(configLevelEnrichment.getLevelUrls());
        label = configLevelEnrichment.getLevelUrlsLabel();
    }
    
    public synchronized String getUrlForLevel(String levelId) {
        checkMapping();
        String m = mapping.get(levelId);
        if (m!=null) {
            return prefix + m;
        } else {
            return null;
        }
    }
    
    public String getLabel() {
        return label;
    }
    
    private Map<String, String> init() throws IOException {
        Map<String, String> map = new HashMap<>();
        if (mappingPath != null) {                       
            lastModification = Files.getLastModifiedTime(mappingPath);
            try(Reader in = new FileReader(mappingPath.toFile());) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setDelimiter(';').setTrim(true).build().parse(in);
            for (CSVRecord record : records) {
                String uuid = record.get(0);
                String path = record.get(1);
                map.put(uuid, path);
            }
            }
            log.info("LevelUrls read, num mappings:{}, path:{}",map.size(), mappingPath);
        }       
        return map;
    }
    
    private void checkMapping() {
        if (mappingPath != null && (System.currentTimeMillis() - lastCheck) > 5000) {
            lastCheck = System.currentTimeMillis();
            Path mappingFile = mappingPath;
            if (!Files.isRegularFile(mappingFile)) {
                return;
            }
            try {
                FileTime lastMod = Files.getLastModifiedTime(mappingFile);
                if (lastModification == null || lastMod.compareTo(lastModification) != 0) {
                    mapping.clear();
                    mapping.putAll(init());
                    log.info("LevelUrls mapping updated from {}", mappingPath);
                }
            } catch (IOException ioEx) {
                log.error("Fail to update LevelUrls from {}", mappingPath, ioEx);
            }
        }
    }

}
