package cz.aron.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.domain.types.dto.ApuEntityTreeView;
import cz.aron.domain.types.dto.ApuEntityView;
import cz.aron.repository.ApuEntityRepository;

@RestController
@RequestMapping("/apu")
public class ApuApi {
	
	private static final Logger log = LoggerFactory.getLogger(ApuApi.class);
	
	private final ApuEntityRepository apuEntityRepository;
	
    private final ObjectMapper objectMapper;
    
    private final String treeCache;
    
	public ApuApi(ApuEntityRepository apuEntityRepository,
			ObjectMapper objectMapper, @Value("${files.treeCache:}") String treeCache) {
		this.apuEntityRepository = apuEntityRepository;
		this.objectMapper = objectMapper;
		this.treeCache = treeCache;
	}

	@GetMapping("/{id}")
	@Transactional
    public ResponseEntity<?> getApu(@PathVariable("id") String apuId) {    	
    	var apu = apuEntityRepository.findByUuid(apuId);
    	if (apu==null) {
    		throw new RuntimeException();
    	}
    	
    	// TODO load full entity
    	apu.getAttachments().size();
    	apu.getDigitalObjects().size();
    	for(var part:apu.getParts()) {
    		part.getChildParts().size();
    	}
    	return ResponseEntity.ok(apu);
    }

    @GetMapping("/{id}/tree")
    @Transactional
    public ResponseEntity<?> getSimpleTree(@PathVariable("id") String apuId) {                        
        if (treeCache!=null) {
            var published = apuEntityRepository.findPublishedByUuid(apuId);
            String timestamp = "";
            if (published!=null) {
                timestamp = "" + published.toEpochSecond(ZoneOffset.UTC);
            }
            var directory = Paths.get(treeCache, apuId.substring(0,2), apuId.substring(2,4));
            var path = directory.resolve(apuId + "_" + timestamp  + ".gz");
            if (Files.isRegularFile(path)) {
                return file(path);
            } else {
                var tmpPath = directory.resolve(apuId+".tmp");                
                ApuEntityTreeView apuEntityTreeView = readTree(apuId);
                try {
                    Files.createDirectories(directory);
                    try (var fos = new FileOutputStream(tmpPath.toFile()); var gzos = new GZIPOutputStream(fos)) {
                        objectMapper.writeValue(gzos, apuEntityTreeView);
                        gzos.flush();
                    }
                    Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Tree cached {}", apuId);                    
                } catch (IOException ioEx) {
                    log.error("Fail to cache tree {}", apuId, ioEx);
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        log.warn("Fail to delete {}", path, e);
                    }
                    try {
                        Files.deleteIfExists(tmpPath);
                    } catch (IOException e) {
                        log.warn("Fail to delete {}", path, e);
                    }
                    return ResponseEntity.ok(apuEntityTreeView);
                }
                return file(path);             
            }
        } else {
            ApuEntityTreeView apuEntityTreeView = readTree(apuId);
            return ResponseEntity.ok(apuEntityTreeView);    
        }
    }
    
	private ApuEntityTreeView readTree(String apuId) {
		var entities = apuEntityRepository.findAllByParentUuid(apuId);
		if (entities.isEmpty()) {
			return null;
		}
		var ids = new HashMap<Long, ApuEntityTreeView>();
		ApuEntityTreeView root = null;
		for (var entity : entities) {
			var newEntity = new ApuEntityTreeView(entity.uuid(), entity.name(), entity.description(), entity.ordr(),
					entity.type(), new ArrayList<>());
			ids.put(entity.apu_id(), root);
			if (root == null) {
				root = newEntity;				
			} else {
				var parent = ids.get(entity.parent_id());
				if (parent == null) {
					throw new IllegalStateException();
				}
				parent.children().add(newEntity);
			}
		}
		return root;
	}

    private ResponseEntity<FileSystemResource> file(Path path) {        
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(HttpHeaders.CONTENT_ENCODING, 
          "gzip");        
        return ResponseEntity.ok()
                .headers(responseHeaders)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new FileSystemResource(path));          
    }
    
/*
    @GetMapping("/labels")
    public List<IdLabelDto> mapNames(@RequestBody List<String> ids) {
        return service.mapNames(ids);
    }
*/
  
/*
    @PostMapping("/listview")
    public Result<ApuEntityView> listView(@Valid @RequestBody(required = false) Params params) {
        
        params = coalesce(params, Params::new);
        
        Result<String> idsResult = apuRepository.getIndex().listIdsByParams(params);
        List<ApuEntityView> items = apuEntityViewStore.listByIds(idsResult.getItems());
        return new Result<>(items, idsResult.getCount(), idsResult.getSearchAfter(), idsResult.getAggregations());
    }
*/
  
/*
    @GetMapping(value = "/{id}/view")
    public ApuEntityView getView(@PathVariable("id") String id) {                        
        ApuEntityView view = apuEntityViewStore.find(id);        
        notNull(view, () -> new MissingObject(ApuEntityView.class, id));
        return view;
    }
*/
    
    @PostMapping(value = "/views")
    public List<ApuEntityView> getViews(@RequestBody List<String> ids) {    	
    	if (ids.size()>100) {
    		throw new IllegalArgumentException();
    	}
    	return apuEntityRepository.findAllByUuids(ids);
    }

}

