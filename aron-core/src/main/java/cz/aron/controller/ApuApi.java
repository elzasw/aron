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
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.api.rest.AronApi;
import cz.aron.api.rest.model.ApuEntity;
import cz.aron.api.rest.model.ApuEntitySimplified;
import cz.aron.api.rest.model.ApuEntityTreeViewDto;
import cz.aron.api.rest.model.Params;
import cz.aron.api.rest.model.SimpleResult;
import cz.aron.domain.types.dto.ApuEntityTreeView;
import cz.aron.indexing.IndexedApu;
import cz.aron.indexing.QueryBuilder;
import cz.aron.indexing.SimpleResultBuilder;
import cz.aron.mapper.ApuEntityMapper;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.ApuEntitySimpleRepository;
import cz.aron.service.ApuService;
import jakarta.validation.Valid;

@RestController
public class ApuApi implements AronApi {

	private static final Logger log = LoggerFactory.getLogger(ApuApi.class);

	private final ApuEntityRepository apuEntityRepository;
	
	private final ApuEntitySimpleRepository apuEntitySimpleRepository;
	
	private final ApuService apuService;

    private final ObjectMapper objectMapper;

    private final String treeCache;

    private final ApuEntityMapper apuEntityMapper;

    private final ElasticsearchOperations elasticsearchOperations;

    private final QueryBuilder queryBuilder;

    private final SimpleResultBuilder simpleResultBuilder;

	public ApuApi(ApuEntityRepository apuEntityRepository, ApuEntitySimpleRepository apuEntitySimpleRepository,
			ApuService apuService,
			ObjectMapper objectMapper, @Value("${files.treeCache:}") String treeCache,
			ApuEntityMapper apuEntityMapper, ElasticsearchOperations elasticsearchOperations,
			QueryBuilder queryBuilder, SimpleResultBuilder simpleResultBuilder) {
		this.apuEntityRepository = apuEntityRepository;
		this.apuEntitySimpleRepository = apuEntitySimpleRepository;
		this.apuService = apuService;
		this.objectMapper = objectMapper;
		this.treeCache = treeCache;
		this.apuEntityMapper = apuEntityMapper;
		this.elasticsearchOperations = elasticsearchOperations;
		this.queryBuilder = queryBuilder;
		this.simpleResultBuilder = simpleResultBuilder;
	}

	@Override
	@Transactional
    public ResponseEntity<ApuEntity> getApu(@PathVariable("id") String apuId) {
    	var apu = apuEntityRepository.findByUuid(apuId);
    	if (apu == null) {
    		throw new RuntimeException();
    	}
    	// trigger lazy collections before transaction closes
    	apu.getAttachments().size();
    	apu.getDigitalObjects().size();
    	for (var part : apu.getParts()) {
    		part.getChildParts().size();
    	}
    	return ResponseEntity.ok()
    			.contentType(MediaType.APPLICATION_JSON)
    			.body(apuEntityMapper.toRest(apu));
    }

    //@GetMapping("/{id}/tree")
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
        responseHeaders.set(HttpHeaders.CONTENT_ENCODING, "gzip");
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
    @GetMapping(value = "/{id}/view")
    public ApuEntityView getView(@PathVariable("id") String id) {
        ApuEntityView view = apuEntityViewStore.find(id);
        notNull(view, () -> new MissingObject(ApuEntityView.class, id));
        return view;
    }
*/

	@Override
	public ResponseEntity<SimpleResult> listView(@Valid Params params) {
		var query = queryBuilder.build(params);
		var hits = elasticsearchOperations.search(query, IndexedApu.class, IndexCoordinates.of("apu"));
		var uuids = hits.getSearchHits().stream().map(h -> h.getId()).collect(Collectors.toList());
		var entities = apuEntitySimpleRepository.findAllByUuidIn(uuids);
		var simplified = entities.stream().map(e -> {
			var s = new ApuEntitySimplified();
			s.setId(e.getUuid());
			s.setName(e.getName());
			s.setDescription(e.getDescription());
			s.setOrder((long) e.getOrder());
			return s;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(simpleResultBuilder.build(hits, simplified));
	}

	@Override
	public ResponseEntity<List<cz.aron.api.rest.model.ApuEntityView>> getViews(List<String> ids) {		
		if (ids.size()>100) {
			throw new IllegalArgumentException("Too big, max 100 ids");
		}
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=1800")
                .contentType(MediaType.APPLICATION_JSON)
		.body(apuService.findAllByUuids(ids));
	}

	@Override
	public ResponseEntity<cz.aron.api.rest.model.ApuEntityView> getView(String id) {
		// TODO check error
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=1800")
                .contentType(MediaType.APPLICATION_JSON)
		.body(apuService.findAllByUuids(List.of(id)).get(0));
	}

	@Override
	public ResponseEntity<SimpleResult> listSimple(@Valid Params params) {
		var query = queryBuilder.build(params);
		var hits = elasticsearchOperations.search(query, IndexedApu.class, IndexCoordinates.of("apu"));
		var uuids = hits.getSearchHits().stream().map(h -> h.getId()).collect(Collectors.toList());
		var entities = apuEntitySimpleRepository.findAllByUuidIn(uuids);
		var simplified = entities.stream().map(e -> {
			var s = new ApuEntitySimplified();
			s.setId(e.getUuid());
			s.setName(e.getName());
			s.setDescription(e.getDescription());
			s.setOrder((long) e.getOrder());
			return s;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(simpleResultBuilder.build(hits, simplified));
	}

	@Override
	public ResponseEntity<SimpleResult> callList(@Valid Params params) {
		var query = queryBuilder.build(params);
		var hits = elasticsearchOperations.search(query, IndexedApu.class, IndexCoordinates.of("apu"));
		var uuids = hits.getSearchHits().stream().map(h -> h.getId()).collect(Collectors.toList());
		var entities = apuEntitySimpleRepository.findAllByUuidIn(uuids);
		var simplified = entities.stream().map(e -> {
			var s = new ApuEntitySimplified();
			s.setId(e.getUuid());
			s.setName(e.getName());
			s.setDescription(e.getDescription());
			s.setOrder((long) e.getOrder());
			return s;
		}).collect(Collectors.toList());
		return ResponseEntity.ok(simpleResultBuilder.build(hits, simplified));
	}
	
	@Override
	public ResponseEntity<List<ApuEntityTreeViewDto>> getRelatedNodes(String id, String direction) {
		List<ApuEntityTreeViewDto> body = null;
		switch (direction) {
		case "before":
			body = apuService.getEntitiesBefore(id);
			break;
		case "after":
			body = apuService.getEntitiesAfter(id);
			break;
		case "under":
			body = apuService.getEntitiesUnder(id);
			break;
		default:
			throw new RuntimeException();
		}
		return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "public, max-age=1800")
				.contentType(MediaType.APPLICATION_JSON).body(body);
	}

	@Override
	public ResponseEntity<Void> test() {
		System.out.println("Test");
		return ResponseEntity.ok().build();
	}

}
