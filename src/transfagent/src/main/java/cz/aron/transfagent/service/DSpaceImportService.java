package cz.aron.transfagent.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import cz.aron.transfagent.config.ConfigDspace;
import cz.aron.transfagent.service.importfromdir.ImportContext;
import cz.aron.transfagent.service.importfromdir.ImportProcessor;

@Service
public class DSpaceImportService implements ImportProcessor {

    private static Logger log = LoggerFactory.getLogger(DSpaceImportService.class);

    @Autowired
    ConfigDspace configDspace;

    public static void main(String[] args) {
        DSpaceImportService service = new DSpaceImportService();
        service.configDspace = new ConfigDspace();
        service.configDspace.setUrl("http://10.2.0.27:8088/rest");
        service.configDspace.setUser("admin@lightcomp.cz");
        service.configDspace.setPassword("admin");
        service.importData(new ImportContext());
    }

    @Override
    public void importData(ImportContext ic) {

        String jsessionId = getJSessionId();
        System.out.println(jsessionId);

    }

    private String getJSessionId() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("email", configDspace.getUser());
        map.add("password", configDspace.getPassword());
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(configDspace.getUrl() + "/login", request, String.class);
        headers = response.getHeaders();
        String cookie = headers.getFirst(HttpHeaders.SET_COOKIE);

        String[] jsessionId = cookie.split(";");
        return jsessionId[0];
    }

}
