package cz.aron.transfagent.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import cz.aron.transfagent.config.ConfigDao;
import cz.aron.transfagent.config.ConfigDspace;
import cz.aron.transfagent.domain.DaoFile;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.domain.EntityStatus;
import cz.aron.transfagent.repository.DaoFileRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import liquibase.util.Validate;

@Service
public class DSpaceImportService implements ImportProcessor {

    private static Logger log = LoggerFactory.getLogger(DSpaceImportService.class);

    @Autowired
    DaoFileRepository daoFileRepository;

    @Autowired
    ConfigDspace configDspace;

    @Autowired
    ConfigDao configDao;

    public static void main(String[] args) throws IOException {
        DSpaceImportService service = new DSpaceImportService();

        service.configDspace = new ConfigDspace();
        //service.configDspace.setUrl("https://demo.dspace.org");
        service.configDspace.setUrl("http://10.2.0.27:8088");
        service.configDspace.setUser("admin@lightcomp.cz");
        service.configDspace.setPassword("admin");
        service.configDspace.setDisabled(false);

        service.configDao = new ConfigDao();
        service.configDao.setPath("C:/temp/transfagent/daos");

        String uuid = "539a0fda-c785-413b-a636-9006192fb538";
        String saveDir = service.configDao.getPath() + "/" + uuid;
        if (!Files.exists(Path.of(saveDir))) {
             Files.createDirectories(Path.of(saveDir));
        }

        List<DspaceFile> files = service.getDspaceFiles(uuid);
        for (DspaceFile file : files) {
            service.saveDspaceFile(saveDir, file);
        }
    }

    @Override
    public void importData(ImportContext ic) {
        if (configDspace.isDisabled()) {
            return;
        }

        var daos = daoFileRepository.findTop1000ByStatusOrderById(DaoState.ACCESSIBLE);
        for (var dao: daos) {
            try {
                importDaoFiles(dao);
            } catch(Exception e) {
                ic.setFailed(true);
                log.error("Dao file not imported: {}", dao.getId(), e);
                return;
            }
            ic.addProcessed();
        }
    }

    private void importDaoFiles(DaoFile dao) {
        String uuid = dao.getUuid().toString();

        String saveDir = configDao.getPath() + "/" + uuid;
        if (!Files.exists(Path.of(saveDir))) {
            try {
                Files.createDirectories(Path.of(saveDir));
            } catch (IOException e) {
                log.error("Error creating directory {}.", saveDir, e);
                throw new RuntimeException("Error creating directory."); 
            }
        }

        List<DspaceFile> files = getDspaceFiles(uuid);
        for (DspaceFile file : files) {
            saveDspaceFile(saveDir, file);
        }
        dao.setState(DaoState.READY);
        daoFileRepository.save(dao);
    }

    private List<DspaceFile> getDspaceFiles(String uuid) {
        List<DspaceFile> files = new ArrayList<>();
        String restUrl = configDspace.getUrl() + "/rest/items/" + uuid + "/bitstreams";
        String responce;
        try {
            RestTemplate restTemplate = new RestTemplate();
            responce = restTemplate.getForObject(restUrl, String.class);
        } catch (Exception e) {
            log.error("Error while accessing dspace {}.", restUrl, e);
            throw new RuntimeException("Error while accessing dspace."); 
        }

        JsonReader jsonReader = Json.createReader(new StringReader(responce));
        JsonArray jsonArray = jsonReader.readArray();

        for (JsonValue value : jsonArray) {
            JsonObject object = value.asJsonObject();

            String name = object.getString("name");
            String retrieveLink = object.getString("retrieveLink");
            int size = object.getInt("sizeBytes");

            Validate.notNull(name, "Název souboru nesmí být prázdný");
            Validate.notNull(retrieveLink, "Odkaz pro dotaz nesmí být prázdný");

            files.add(new DspaceFile(name, retrieveLink, size));
        }
        return files;
    }

    private void saveDspaceFile(String saveDir, DspaceFile file) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(saveDir + "/" + file.getName())) {
            URL url = new URL(configDspace.getUrl() + file.getRetrieveLink());
            fileOutputStream
                .getChannel()
                .transferFrom(Channels.newChannel(url.openStream()), 0, Long.MAX_VALUE);

            log.info("File {} successfully transferred from {}.", file.getName(), file.getRetrieveLink());
        } catch (IOException e) {
            log.error("File {} transfer error from {}.", file.getName(), file.getRetrieveLink(), e);
            throw new RuntimeException("File transfer error."); 
        }
    }

    private String getJSessionId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("email", configDspace.getUser());
        map.add("password", configDspace.getPassword());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(configDspace.getUrl() + "/rest/login", request, String.class);
        } catch (Exception e) {
            log.error("Error while accessing DSpace.", e);
            throw new RuntimeException("Error while accessing DSpace."); 
        }
        headers = response.getHeaders();
        String cookie = headers.getFirst(HttpHeaders.SET_COOKIE);
        String[] jsessionId = cookie.split(";");

        return jsessionId[0];
    }

    public class DspaceFile {
        final String name;
        final String retrieveLink;
        final int size;

        public DspaceFile(String name, String retrieveLink, int size) {
            this.name = name;
            this.retrieveLink = retrieveLink;
            this.size = size;
        }

        public String getName() {
            return name;
        }

        public String getRetrieveLink() {
            return retrieveLink;
        }

        public int getSize() {
            return size;
        }
    }

}
