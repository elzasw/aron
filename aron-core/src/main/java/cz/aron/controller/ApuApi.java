package cz.aron.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import cz.aron.api.rest.model.ApuEntityView;
import cz.aron.api.rest.model.Params;
import cz.aron.api.rest.model.ResultRowItem;
import cz.aron.api.rest.model.ResultRowItemValue;
import cz.aron.api.rest.model.SimpleResult;
import cz.aron.api.rest.model.StructuredResult;
import cz.aron.api.rest.model.StructuredResults;
import cz.aron.domain.types.dto.ApuEntityTreeView;
import cz.aron.indexing.OldApiSearch;
import cz.aron.indexing.SimpleResultBuilder;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.service.ApuService;
import cz.aron.service.ApuService.Data;
import cz.aron.service.ApuService.NotModified;
import cz.aron.service.ApuService.Result;
import jakarta.validation.Valid;

@RestController
public class ApuApi implements AronApi {

	private static final Logger log = LoggerFactory.getLogger(ApuApi.class);

	private static final long CACHE_MAX_AGE_SECONDS = 1800;

	private final ApuEntityRepository apuEntityRepository;

	private final ApuService apuService;

    private final ObjectMapper objectMapper;

    private final String treeCache;

    private final OldApiSearch oldApiSearch;

    private final SimpleResultBuilder simpleResultBuilder;

	public ApuApi(ApuEntityRepository apuEntityRepository,
			ApuService apuService,
			ObjectMapper objectMapper, @Value("${files.treeCache:}") String treeCache,
			OldApiSearch oldApiSearch, SimpleResultBuilder simpleResultBuilder) {
		this.apuEntityRepository = apuEntityRepository;
		this.apuService = apuService;
		this.objectMapper = objectMapper;
		this.treeCache = treeCache;
		this.oldApiSearch = oldApiSearch;
		this.simpleResultBuilder = simpleResultBuilder;
	}

    @Override
	public ResponseEntity<ApuEntity> getApu(UUID apuId, String ifNoneMatch, String ifModifiedSince) {
    	var apu = apuService.getApuEntity(apuId, ifNoneMatch, ifModifiedSince);
    	if (apu instanceof NotModified<ApuEntity> notModified) {
    		return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
    				.cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
    				.eTag(notModified.eTag())
    				.lastModified(notModified.lastModified())
    				.build();
    	} else if (apu instanceof Data<ApuEntity> data) {
    		return ResponseEntity.ok()
        			.cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
        			.contentType(MediaType.APPLICATION_JSON)
        			.eTag(data.eTag())
        			.lastModified(data.lastModified())
        			.body(data.value());
    	}
    	throw new IllegalStateException();
    }

	//@GetMapping("/{id}/tree")
    @Transactional
    public ResponseEntity<?> getSimpleTree(@PathVariable("id") String apuId) {
        if (treeCache!=null) {
            var published = apuEntityRepository.findPublishedByUuid(java.util.UUID.fromString(apuId));
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
		var entities = apuEntityRepository.findAllByParentUuid(java.util.UUID.fromString(apuId));
		if (entities.isEmpty()) {
			return null;
		}
		var ids = new HashMap<Long, ApuEntityTreeView>();
		ApuEntityTreeView root = null;
		for (var entity : entities) {
			var newEntity = new ApuEntityTreeView(entity.uuid().toString(), entity.name(), entity.description(), entity.ordr(),
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
		return ResponseEntity.ok(buildSimpleResult(params));
	}

	private SimpleResult buildSimpleResult(Params params) {
		var searchResult = oldApiSearch.search(params);
		var uuids = searchResult.uuids().stream().map(java.util.UUID::fromString).collect(Collectors.toList());
		var simplified = apuEntityRepository.findDtosByUuidIn(uuids).stream().map(d -> {
			var s = new ApuEntitySimplified();
			s.setId(d.uuid().toString());
			s.setName(d.name());
			s.setDescription(d.description());
			s.setOrder((long) d.order());
			return s;
		}).collect(Collectors.toList());
		return simpleResultBuilder.build(searchResult, simplified);
	}

	@Override
	public ResponseEntity<List<cz.aron.api.rest.model.ApuEntityView>> getViews(List<UUID> ids) {
		if (ids.size()>100) {
			throw new IllegalArgumentException("Too big, max 100 ids");
		}
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePrivate())
                .contentType(MediaType.APPLICATION_JSON)
		.body(apuService.findAllByUuids(ids));
	}

	@Override
	public ResponseEntity<ApuEntityView> getView(UUID id, String ifNoneMatch, String ifModifiedSince) {
		// TODO check error
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePrivate())
                .contentType(MediaType.APPLICATION_JSON)
		.body(apuService.findAllByUuids(List.of(id)).get(0));
	}

	@Override
	public ResponseEntity<SimpleResult> listSimple(@Valid Params params) {
		return ResponseEntity.ok(buildSimpleResult(params));
	}

	@Override
	public ResponseEntity<SimpleResult> callList(@Valid Params params) {
		return ResponseEntity.ok(buildSimpleResult(params));
	}

	@Override
	public ResponseEntity<List<ApuEntityTreeViewDto>> getRelatedNodes(UUID id, String direction, String ifNoneMatch,
			String ifModifiedSince) {
		Result<List<ApuEntityTreeViewDto>> result;
		switch (direction) {
		case "before":
			result = apuService.getEntitiesBefore(id, ifNoneMatch, ifModifiedSince);
			break;
		case "after":
			result = apuService.getEntitiesAfter(id, ifNoneMatch, ifModifiedSince);
			break;
		case "under":
			result = apuService.getEntitiesUnder(id, ifNoneMatch, ifModifiedSince);
			break;
		default:
			throw new RuntimeException();
		}
		if (result instanceof NotModified<List<ApuEntityTreeViewDto>> notModified) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
					.cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
					.eTag(notModified.eTag())
					.lastModified(notModified.lastModified())
					.build();
		} else if (result instanceof Data<List<ApuEntityTreeViewDto>> data) {
			return ResponseEntity.ok()
					.cacheControl(CacheControl.maxAge(CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
					.contentType(MediaType.APPLICATION_JSON)
					.eTag(data.eTag())
					.lastModified(data.lastModified())
					.body(data.value());
		}
		throw new IllegalStateException();
	}

	@Override
	public ResponseEntity<Void> test() {
		System.out.println("Test");
		return ResponseEntity.ok().build();
	}

	@Override
	public ResponseEntity<StructuredResults> listResults(@Valid Params params) {
		var searchResult = oldApiSearch.search(params);
		var uuids = searchResult.uuids().stream().map(java.util.UUID::fromString).collect(Collectors.toList());
		Map<String, StructuredResult> byId = apuService.findAllResultsByUuidIn(uuids);

		var result = new StructuredResults();
		result.setAggregations(searchResult.aggregations());
		result.setItems(searchResult.uuids().stream()
                .filter(byId::containsKey)
                .map(uuid->{
                	var structuredResult = byId.get(uuid);
                	if (structuredResult!=null) {
            			return structuredResult;
            		} else {
            			return createEmptyResult(uuid);
            		}
                })
                .collect(Collectors.toList()));
		result.setCount(searchResult.total());
		if (searchResult.searchAfter() != null && !searchResult.searchAfter().isEmpty()) {
			result.setSearchAfter(new ArrayList<>(searchResult.searchAfter()));
		}
		return ResponseEntity.ok(result);
	}
	
	
    public static StructuredResult createEmptyResult(String id) {
        var resRowItemValue = new ResultRowItemValue();
        resRowItemValue.setV("Prazdny vysledek");
        var resRowItem = new ResultRowItem();
        resRowItem.setT("UNKNOWN");
        resRowItem.addVItem(resRowItemValue);
        var res = new StructuredResult();
        res.setId(id);
        res.setT("UNKNOWN");
        res.addLItem(Arrays.asList(resRowItem));
        return res;
    }

}
