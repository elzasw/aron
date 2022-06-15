package cz.aron.transfagent.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.json.Json;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import cz.aron.transfagent.config.ConfigDao;
import cz.aron.transfagent.config.ConfigDspace;
import cz.aron.transfagent.domain.Dao;
import cz.aron.transfagent.domain.DaoState;
import cz.aron.transfagent.repository.ApuSourceRepository;
import cz.aron.transfagent.repository.DaoRepository;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;
import cz.aron.transfagent.service.importfromdir.TransformService;
import cz.aron.transfagent.transformation.DSpaceConsts;
import liquibase.util.Validate;

// TODO change to DaoImporter
//@Service
public class DSpaceImportService implements ImportProcessor {

    private static Logger log = LoggerFactory.getLogger(DSpaceImportService.class);

    @Autowired
    ApuSourceRepository apuSourceRepository;

    @Autowired
    DaoRepository daoRepository;

    @Autowired
    FileImportService importService;

    @Autowired
    TransformService transformService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    ConfigDspace configDspace;

    @Autowired
    ConfigDao configDao;

    public static void main(String[] args) throws IOException {
        var service = new DSpaceImportService();

        service.configDspace = new ConfigDspace();
        service.configDspace.setUrl("http://10.2.0.27:8088");
        service.configDspace.setUser("admin@lightcomp.cz");
        service.configDspace.setPassword("admin");
        service.configDspace.setDisabled(false);

        service.configDao = new ConfigDao();
        service.configDao.setDir("C:/temp/transfagent/daos");

        // http://10.2.0.27:8088/rest/handle/<handle>
        // http://10.2.0.27:8088/rest/items/<uuid>/bitstreams

        var itemUuid = "61259486-3786-4877-85b5-f845ee038132";
        var saveDir = Files.createDirectories(Path.of(service.configDao.getDir()).resolve(itemUuid));
        var sessionId = service.getJSessionId();
        service.downloadBitstreamInfo(saveDir, sessionId);
    }

    @PostConstruct
    void init() {
        importService.registerImportProcessor(this);
    }
    
    @Override
    public void importData(ImportContext ic) {
        if (configDspace.isDisabled()) {
            return;
        }

        var daos = daoRepository.findTop1000ByStateOrderById(DaoState.ACCESSIBLE);
        for (var dao : daos) {
            try {
                importDaoFiles(dao);
            } catch(Exception e) {
                ic.setFailed(true);
                log.error("Dao file not imported: {}.", dao.getId(), e);
                return;
            }
            ic.addProcessed();
        }
    }

    private void importDaoFiles(Dao dao) {
        log.info("Importing DAO, daoId: {} (handle: {}, uuid: {})", dao.getId(), dao.getHandle(), dao.getUuid());

        // get authentication cookie
        var sessionId = getJSessionId();

        // check or get uuid
        String uuid;
        if (dao.getUuid() == null) {
            uuid = getItemIdFromHandle(dao.getHandle(), sessionId);
            dao.setUuid(UUID.fromString(uuid));
        } else {
            uuid = dao.getUuid().toString();
        }

        // TODO: use dates in path to split in multiple dirs
        var saveDir = Path.of(configDao.getDir()).resolve(uuid);
        if (!Files.exists(saveDir)) {
            try {
                Files.createDirectories(saveDir);
            } catch (IOException e) {
                log.error("Error creating directory {}.", saveDir, e);
                throw new RuntimeException("Error creating directory."); 
            }
        }

        downloadBitstreamInfo(saveDir, sessionId);

        try {
            if(!transformService.transform(saveDir, null)) {
                // delete empty dir
            }
        } catch (Exception e) {
            log.error("Error in transform path={}.", saveDir, e);
            throw new RuntimeException("Error in transform path.", e);
        }

        // save to db
        dao.setDataDir(uuid);
        dao.setState(DaoState.READY);
        dao.setDownload(false);
        transactionTemplate.executeWithoutResult(t -> saveDao(t, dao));
    }

    /**
     * Saving files received from DSpace
     * 
     * @param saveDir
     * @param sessionId
     */
    private void downloadBitstreamInfo(Path saveDir, String sessionId) {
        var jsonValue = getBitstreamsJsonValue(saveDir.getFileName().toString(), sessionId);
        var bitstreamJsonPath = saveDir.resolve(DSpaceConsts.BITSTREAM_JSON);
        try (OutputStream output = new FileOutputStream(bitstreamJsonPath.toFile())) {
           var jsonWriter = Json.createWriter(output);
           jsonWriter.write(jsonValue);
        } catch (IOException e) {
            log.error("Error writing file={}.", bitstreamJsonPath, e);
            throw new RuntimeException("Error writing file " + bitstreamJsonPath.toString(), e);
        }

        for (var file : getDspaceFiles(jsonValue)) {
            saveDspaceFile(saveDir, file, sessionId);
        }
    }

    /**
     * Read item from handle
     * 
     * @param handle
     * @return Return content of property uuid:
     */
    private String getItemIdFromHandle(String handle, String sessionId) {
        log.debug("DSpace: Reading item for handle: {}", handle);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionId);
        var restTemplate = new RestTemplate();
        var restUrl = configDspace.getUrl() + "/rest/handle/" + handle;
        try {
            ResponseEntity<String> response = restTemplate.exchange(restUrl, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
            log.debug("DSpace response: {}", response.getBody());

            var jsonReader = Json.createReader(new StringReader(response.getBody()));
            var jsonObj = jsonReader.readObject();
            var uuid = jsonObj.getString("uuid");

            /*var i = response.indexOf("<UUID>");
            var j = response.indexOf("</UUID>"); 
            var uuid = response.substring(i+6, j);
            */
            log.info("Received uuid for handle {} is ", handle, uuid);

            return uuid;
        } catch (Exception e) {
            log.error("Error while accessing dspace {}.", restUrl, e);
            throw new RuntimeException("Error while accessing dspace.");
        }
    }

    /**
     * Save Dao to DB
     * 
     * @param t active transaction
     * @param dao Dao to be saved
     */
    private void saveDao(TransactionStatus t, Dao dao) {
        log.debug("Saving Dao to DB, daoId: {}", dao.getId());

        var dbDao = daoRepository.findById(dao.getId())
                .orElseThrow(
                    ()-> {
                        log.error("Dao not exists in DB, id: {}", dao.getId());
                        return new RuntimeException("Dao not exists");
                    }
                );
        dbDao.setDataDir(dao.getDataDir());
        if (dbDao.getUuid() == null) {
            // Dao has no uuid -> connected apusource have to be reindexed
            var apuSource = dbDao.getApuSource();
            apuSource.setReimport(true);
            apuSourceRepository.save(apuSource);
        }
        dbDao.setUuid(dao.getUuid());
        dbDao.setState(dao.getState());
        dbDao.setTransferred(false);
        dbDao.setDownload(dao.isDownload());
        daoRepository.save(dbDao);
    }

    /**
     * Get json data of bitstreams by Item UUID
     * 
     * @param uuid of item
     * @param sessionId authentication cookie
     * @return JsonValue
     */
    private JsonValue getBitstreamsJsonValue(String uuid, String sessionId) {
        log.debug("Getting bistreams from DSpace uuid: {}", uuid);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionId);

        List<DspaceFile> files = new ArrayList<>();
        var restUrl = configDspace.getUrl() + "/rest/items/" + uuid + "/bitstreams";
        var restTemplate = new RestTemplate();
        ResponseEntity<String> responce;
        try {
            responce = restTemplate.exchange(restUrl, HttpMethod.GET, new HttpEntity<String>(headers), String.class);
        } catch (Exception e) {
            log.error("Error while accessing dspace {}.", restUrl, e);
            throw new RuntimeException("Error while accessing dspace.");
        }

        var jsonReader = Json.createReader(new StringReader(responce.getBody()));
        return jsonReader.readValue();
    }

    /**
     * Get data of all files (bitstreams) by Item UUID
     * @param jsonValue
     * @return
     */
    private List<DspaceFile> getDspaceFiles(JsonValue jsonValue) {
        List<DspaceFile> files = new ArrayList<>();
        var filterBundle = configDspace.getBundleName();
        for (var value : jsonValue.asJsonArray()) {
            var object = value.asJsonObject();

            if (filterBundle == null || filterBundle.equals(object.getString(DSpaceConsts.BUNDLE_NAME))) {
                var name = object.getString(DSpaceConsts.NAME);
                var retrieveLink = object.getString(DSpaceConsts.RETRIEVE_LINK);
                int size = object.getInt(DSpaceConsts.SIZE_BYTES);

                Validate.notNull(name, "Název souboru nesmí být prázdný");
                Validate.notNull(retrieveLink, "Odkaz pro dotaz nesmí být prázdný");

                files.add(new DspaceFile(name, retrieveLink, size));
            }
        }
        return files;
    }

    /**
     * Save file, reading from Http authenticated requests
     * 
     * @param saveDir folder for saving
     * @param file name and link to the file
     * @param sessionId authentication cookie
     */
    private void saveDspaceFile(Path saveDir, DspaceFile file, String sessionId) {
        var filePath = saveDir.resolve(file.getName());

        log.debug("Downloading file from DSpace, link: {}, targetFile: {}", file.getRetrieveLink(), filePath.toString());

        HttpURLConnection connect;
        try (OutputStream output = new FileOutputStream(filePath.toFile())) {
            connect = (HttpURLConnection) new URL(configDspace.getUrl() + file.getRetrieveLink()).openConnection();
            connect.setRequestMethod("GET");
            connect.addRequestProperty("Cookie", sessionId);
            connect.getInputStream().transferTo(output);
        } catch (IOException e) {
            log.error("File transfer error, link: {}, targetFile: {}", file.getRetrieveLink(), file.getName(), e);
            throw new RuntimeException("File transfer error."); 
        }
    }

    /**
     * Returns a JSESSIONID cookie, that can be used for authenticated requests
     * 
     * @return authentication cookie
     */
    private String getJSessionId() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("email", configDspace.getUser());
        map.add("password", configDspace.getPassword());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        var restTemplate = new RestTemplate();
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(configDspace.getUrl() + "/rest/login", request, String.class);
        } catch (Exception e) {
            log.error("Error while accessing DSpace.", e);
            throw new RuntimeException("Error while accessing DSpace."); 
        }
        headers = response.getHeaders();
        var cookie = headers.getFirst(HttpHeaders.SET_COOKIE);

        return cookie.split(";")[0];
    }

    /**
     * Class for saving information about a file
     */
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

    
    /*
    public void updateDaos(ApuSource apuSource, Set<String> daoRefs) {
        var daos = daoRepository.findByApuSource(apuSource);
        Map<String, Dao> daoLookup = new HashMap<>();
        for (var dao : daos) {
            if(StringUtils.isNotBlank(dao.getHandle())) {
                daoLookup.put(dao.getHandle(), dao);
            }
        }

        for (String daoHandle : daoRefs) {
            var dao = daoLookup.get(daoHandle);
            if (dao != null) {
                if (dao.getState() == DaoState.INACCESSIBLE) {
                    dao.setState(DaoState.ACCESSIBLE);
                    daoRepository.save(dao);
                }
                daoLookup.remove(dao);
            } else {
                // store new dao
                dao = new Dao();
                dao.setHandle(daoHandle);
                dao.setApuSource(apuSource);
                dao.setTransferred(false);
                dao.setState(DaoState.ACCESSIBLE);
                daoRepository.save(dao);
            }
        }
        // zbyle objekty musi byt zneplatneny
        for (var dao : daoLookup.values()) {
            dao.setState(DaoState.INACCESSIBLE);
            daoRepository.save(dao);
        }
    }
    */

}
