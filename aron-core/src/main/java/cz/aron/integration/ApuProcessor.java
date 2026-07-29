package cz.aron.integration;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.Attachment;
import cz.aron.apux._2020.DescItems;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.ItemEnum;
import cz.aron.apux._2020.ItemJson;
import cz.aron.apux._2020.ItemLink;
import cz.aron.apux._2020.ItemRef;
import cz.aron.apux._2020.ItemString;
import cz.aron.apux._2020.Part;
import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.domain.ApuAttachment;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.DataType;
import cz.aron.domain.DigitalObject;
import cz.aron.domain.DigitalObjectType;
import cz.aron.domain.Relation;
import cz.aron.domain.UniversalDate;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.indexing.IndexingService;
import cz.aron.repository.ApuEntityRepository;
import cz.aron.repository.ApuSourceRepository;
import cz.aron.repository.DaoRepository;
import cz.aron.repository.RelationRepository;
import cz.aron.service.ApuRequestQueue;
import cz.aron.service.ApuService;
import cz.aron.service.IdService;
import jakarta.persistence.EntityManager;

@Service
public class ApuProcessor {

	private static final Logger log = LoggerFactory.getLogger(ApuProcessor.class);

	private final ApuSourceRepository apuSourceRepository;
	private final ApuEntityRepository apuEntityRepository;
	private final DaoRepository daoRepository;
	private final RelationRepository relationRepository;
	private final ApuRequestQueue apuRequestQueue;
	private final TypesHolder typesHolder;
	private final FileInputProcessor fileInputProcessor;
	private final ObjectMapper objectMapper;
	private final EntityManager entityManager;
	private final IndexingService indexingService;
	private final ApuService apuService;
	private final IdService idService;

	private Map<String, ApuEntity> saveCache = new LinkedHashMap<>(); // maintain order so that parent always comes
																		// before child
	private Set<RelationKey> relationsAddCache = new HashSet<>();	// set of relations to be added to database
	private Set<UUID> apusToHaveIncomingRelsUpdated = new HashSet<>();
	private Map<String, LevelStats> apuIdsStates = new HashMap<>();
	private Map<String, DigitalObject> existingDaos = new HashMap<>();

	private static final int CACHE_SIZE = 100;

	private int apuOrderCounter;

	private long partIdSeq; // sequential id for parts/items within a single APU

	public ApuProcessor(ApuSourceRepository apuSourceRepository, ApuEntityRepository apuEntityRepository,
			DaoRepository daoRepository, RelationRepository relationRepository, ApuRequestQueue apuRequestQueue,
			TypesHolder typesHolder, FileInputProcessor fileInputProcessor, ObjectMapper objectMapper,
			EntityManager entityManager, IndexingService indexingService, ApuService apuService,
			IdService idService) {
		this.apuSourceRepository = apuSourceRepository;
		this.apuEntityRepository = apuEntityRepository;
		this.daoRepository = daoRepository;
		this.relationRepository = relationRepository;
		this.apuRequestQueue = apuRequestQueue;
		this.typesHolder = typesHolder;
		this.fileInputProcessor = fileInputProcessor;
		this.objectMapper = objectMapper;
		this.entityManager = entityManager;
		this.indexingService = indexingService;
		this.apuService = apuService;
		this.idService = idService;
	}

	@Transactional
	public void processApuAndFiles(Path apuSrcPath, Map<String, Path> filesMap) {

		preprocess(apuSrcPath);
		
		try (ApuSourceBatchReader reader = new ApuSourceBatchReader(apuSrcPath);) {
			log.debug("Processing apu source {}", reader.getUuid());
			var ourApuSource = apuSourceRepository.findByUuid(UUID.fromString(reader.getUuid()));
			if (ourApuSource == null) {
				ourApuSource = new cz.aron.domain.ApuSource();
				ourApuSource.setId(idService.getNextApuSourceId());
				ourApuSource.setUuid(UUID.fromString(reader.getUuid()));
			} else {
				removeExistingApusAndRelations(ourApuSource.getId(), reader.getUuid());
			}
			// ourApuSource.setData(metadata);
			ourApuSource.setPublished(LocalDateTime.now());
			ourApuSource = apuSourceRepository.save(ourApuSource);

			// hold only reference, ApuSource.data are potentialy large
			var apuSourceRef = apuSourceRepository.getReferenceById(ourApuSource.getId());
			apuOrderCounter = 0;
			reader.process(apus -> {
				fillDaoCache(apus.stream().filter(apu -> apu.getDaos() != null)
						.flatMap(apu -> apu.getDaos().getUuid().stream()).collect(Collectors.toList()));
				for (Apu apu : apus) {
					processApu(apu, apuSourceRef, filesMap);
				}
				flush(apuSourceRef);
				log.debug("Processing apu source {}, process chunk of size {}", reader.getUuid(), apus.size());
			}, CACHE_SIZE);
			removeNotUsedRelations(apuSourceRef);
		} catch (Exception e) {
			log.error("Fail to import apusource ", e);
			throw new RuntimeException(e);
		} finally {
			clearInternalState();
		}
	}

	private void removeExistingApusAndRelations(long apuSourceId, String uuid) {
		
		// remove from index
		indexingService.deleteApus(apuSourceId);

		// remove relations from Dao to ApuEntity
		var numDisconnected = daoRepository.disconnectDaosByApuSourceId(apuSourceId);

		// mark Relation to be removed when not used anymore
		var numDeletedRelations = relationRepository.markToRemoveByApuSourceId(apuSourceId);
		log.debug("Processing apu source {}, disconnect {} existing daos, delete {} relations", uuid,
				numDisconnected, numDeletedRelations);
		apuEntityRepository.flush();

		// remove all ApuEntity from bottom to top to not breach referential integrity
		var idParentIds = apuEntityRepository.findIdParentIdByApuSourceId(apuSourceId);
		Map<Long, Long> idToParentIdMap = new HashMap<>();
		for (var idParentId : idParentIds) {
			idToParentIdMap.put(idParentId.id(), idParentId.parentId());
		}
		var topToBottomIds = new LinkedHashSet<Long>();
		while (!idToParentIdMap.isEmpty()) {
			var iterator = idToParentIdMap.entrySet().iterator();
			while (iterator.hasNext()) {
				var next = iterator.next();
				if (next.getValue() == null || topToBottomIds.contains(next.getValue())) {
					topToBottomIds.add(next.getKey());
					iterator.remove();
				}
			}
		}
		// Reverse
		var apuIdsToDelete = new ArrayList<>(topToBottomIds);
		Collections.reverse(apuIdsToDelete);
		apuEntityRepository.deleteAllById(apuIdsToDelete);
		apuEntityRepository.flush();
		log.debug("Processing apu source {}, original data deleted", uuid);
		entityManager.clear();		
	}
	
    /**
     * First pass over the APU XML source: computes {@code depth} and {@code pos} for every APU
     * and populates {@link #apuIdsStates} so that the main processing pass can assign these
     * values without requiring a second database lookup.
     *
     * <p>The algorithm uses a fictive root node (depth&nbsp;0, pos&nbsp;0) as the anchor for
     * top-level APUs. For each APU encountered:
     * <ol>
     *   <li>Resolves the parent via {@link #apuIdsStates} (throws if the parent UUID is unknown,
     *       which would indicate an ordering violation in the source file).</li>
     *   <li>Increments the parent's {@code childCnt} and derives
     *       {@code depth = parent.depth + 1} and {@code pos = parent.childCnt}.</li>
     *   <li>Stores the resulting {@link LevelStats} in {@link #apuIdsStates} keyed by the APU UUID.</li>
     * </ol>
     *
     * <p>On failure the internal state is cleared via {@link #clearInternalState()} so that a
     * subsequent import attempt starts clean.
     *
     * @param apuSrcPath path to the APU XML source file
     * @throws RuntimeException if a referenced parent UUID is not present in the source file,
     *                          or if the XML cannot be read
     */
    private void preprocess(Path apuSrcPath) {
    	var fictiveRoot = new LevelStats();
    	fictiveRoot.depth = 0;
    	fictiveRoot.pos = 0;
    	boolean clearState = true;	
    	try(ApuSourceBatchReader reader = new ApuSourceBatchReader(apuSrcPath);) {
    		log.debug("Processing first phase apu source {}", reader.getUuid());    		
    		reader.process(apus->{
    			for(var apu:apus) {
    				var levelStats = new LevelStats();
    				var parentId = apu.getPrnt();
    				LevelStats parent;
    				if (parentId!=null) {
    					parent = apuIdsStates.get(parentId);
    					if (parent==null) {
    						log.error("Parent {} not exist", parentId);
    						throw new RuntimeException("parent not exist"+parentId);
    					}    					    				
    				} else {
    					parent = fictiveRoot;
    				}
    				parent.childCnt++;
					levelStats.depth = parent.depth + 1;
					levelStats.pos = parent.childCnt;    				
    				apuIdsStates.put(apu.getUuid(), levelStats);
    			}
    		}, CACHE_SIZE);
    		clearState = false;
    	 } catch (Exception e) {
             log.error("Fail to import apusource, preprocess phase ", e);
             throw new RuntimeException(e);
    	} finally {
    		if (clearState) {
    			clearInternalState();
    		}
    	}
    }

	private void clearInternalState() {
		saveCache.clear();
		relationsAddCache.clear();
		apusToHaveIncomingRelsUpdated.clear();
		apuIdsStates.clear();
		existingDaos.clear();
	}

	@Scheduled(fixedDelay = 60000)
	public void sendRequests() {
		try {
			while (apuRequestQueue.sendRequestsBatch())
				;
		} catch (Exception e) {
			log.error("Fail to send apu requests batch", e);
		}
	}

	public void processTestingInputStream(Path path) throws IOException {
		processApuAndFiles(path, null);
	}

	public void processApu(Apu apu, cz.aron.domain.ApuSource apuSource, Map<String, Path> filesMap) {
		
		var levelState = apuIdsStates.get(apu.getUuid());
		
		ApuEntity apuEntity = new ApuEntity();
		apuEntity.setId(idService.getNextApuEntityId());
		apuEntity.setUuid(UUID.fromString(apu.getUuid()));
		apuEntity.setName(apu.getName());
		apuEntity.setIndexedName(apu.getIndexedName());
		apuEntity.setOrder(++apuOrderCounter);
		apuEntity.setDescription(apu.getDesc());
		apuEntity.setResult(apu.getResult());
		apuEntity.setPermalink(apu.getPrmLnk());
		apuEntity.setType(cz.aron.domain.ApuType.valueOf(apu.getType().name().toUpperCase())); // fixme names don't match
        apuEntity.setChildCnt(levelState.childCnt);
        apuEntity.setPos(levelState.pos);
        apuEntity.setDepth(levelState.depth);
        apuEntity.setIndexed(apu.isIndexed()==null||Boolean.TRUE.equals(apu.isIndexed())); // defaultni hodnota je true
        apuEntity.setReindex(false);
		
		if (apu.getPrnt() != null) {
			ApuEntity parentApu = saveCache.get(apu.getPrnt());
			if (parentApu == null) {
            	var parentLevelStats =  apuIdsStates.get(apu.getPrnt());
                if (parentLevelStats==null||!parentLevelStats.processed) {
                    throw new RuntimeException("parent apu not found yet");
                }
                parentApu = apuEntityRepository.findByUuid(UUID.fromString(apu.getPrnt()));
			}
			apuEntity.setParent(parentApu);
		}
		apuEntity.setSource(apuSource);
		if (apu.getPrts() != null) {
			processParts(apu.getPrts().getPart(), apuEntity);
		}
		processAttachments(apu.getAttchs(), apuEntity, filesMap);
		if (apu.getDaos() != null) {
			int i = 0;
			for (String daoUuid : apu.getDaos().getUuid()) {
				DigitalObject insertedDao = existingDaos.get(daoUuid);
				if (insertedDao == null) {
					insertedDao = new DigitalObject();
					insertedDao.setId(idService.getNextDigitalObjectId());
					insertedDao.setUuid(UUID.fromString(daoUuid));
				}
				insertedDao.setApu(apuEntity);
				insertedDao.setOrder(++i);
				insertedDao.setPublished(LocalDateTime.now());
				apuEntity.getDigitalObjects().add(insertedDao);
			}
		}
		apuEntity.setHasAttachments(!apuEntity.getAttachments().isEmpty());
		levelState.processed = true;
		saveCache.put(apu.getUuid(), apuEntity);
		apusToHaveIncomingRelsUpdated.add(apuEntity.getUuid());
		recordRelations(apuEntity);
		//apuRequestQueue.removeForApuId(apuEntity.getUuid());
	}

	private void processParts(List<Part> parts, ApuEntity apuEntity) {
		partIdSeq = 0;
		Map<String, ApuPart> processedPartCache = new HashMap<>();
		for (Part part : parts) {
			ApuPart apuPart = new ApuPart();
			apuPart.setId(String.valueOf(++partIdSeq));
			apuPart.setValue(part.getValue());
			apuPart.setType(part.getType().replace("_", "~"));
			Object prnt = part.getPrnt();
			if (prnt != null) {
				String parentPartId = ((Part) prnt).getId();
				ApuPart parentPart = processedPartCache.get(parentPartId);
				if (parentPart == null) {
					throw new RuntimeException("parent part not processed yet, move the connecting at the end");
				}
				parentPart.getChildParts().add(apuPart);
			} else {
				apuEntity.getParts().add(apuPart);
				processedPartCache.put(part.getId(), apuPart);
			}
			processPartItems(part.getItms(), apuPart);
		}
	}

	private void processPartItems(DescItems itms, ApuPart apuPart) {
		for (Object o : itms.getStrOrLnkOrEnm()) {
			ApuPartItem item = new ApuPartItem();
			item.setId(String.valueOf(++partIdSeq));
			if (o instanceof ItemString) {
				ItemString itemString = (ItemString) o;
				item.setType(itemString.getType().replace("_", "~"));
				item.setValue(itemString.getValue());
				item.setVisible(itemString.isVisible() == null || itemString.isVisible());
			} else if (o instanceof ItemLink) {
				ItemLink itemLink = (ItemLink) o;
				item.setType(itemLink.getType().replace("_", "~"));
				item.setValue(itemLink.getName());
				item.setHref(itemLink.getLink());
				item.setVisible(itemLink.isVisible() == null || itemLink.isVisible());
			} else if (o instanceof ItemEnum) {
				ItemEnum itemEnum = (ItemEnum) o;
				item.setType(itemEnum.getType().replace("_", "~"));
				item.setValue(itemEnum.getValue());
				item.setVisible(itemEnum.isVisible() == null || itemEnum.isVisible());
			} else if (o instanceof ItemRef) {
				ItemRef itemRef = (ItemRef) o;
				item.setType(itemRef.getType().replace("_", "~"));
				item.setValue(itemRef.getValue());
				item.setVisible(itemRef.isVisible() == null || itemRef.isVisible());
				if (itemRef.getType().equals("ORIGINATOR_REF") || itemRef.getType().equals("AP_REF")) { // only archival
																										// entities
					apuRequestQueue.add(itemRef.getValue());
				}
			} else if (o instanceof ItemDateRange) {
				ItemDateRange itemDateRange = (ItemDateRange) o;
				item.setType(itemDateRange.getType().replace("_", "~"));
				UniversalDate universalDate = new UniversalDate();
				universalDate.setFormat(itemDateRange.getFmt());
				universalDate.setFrom(itemDateRange.getF());
				universalDate.setTo(itemDateRange.getTo());
				universalDate.setValueFromEstimated(itemDateRange.isFe() != null && itemDateRange.isFe());
				universalDate.setValueToEstimated(itemDateRange.isToe() != null && itemDateRange.isToe());
				try {
					item.setValue(objectMapper.writeValueAsString(universalDate));
				} catch (JsonProcessingException e) {
					throw new RuntimeException(e);
				}
				item.setVisible(itemDateRange.isVisible() == null || itemDateRange.isVisible());
			} else if (o instanceof ItemJson) {
				ItemJson itemJson = (ItemJson) o;
				item.setType(itemJson.getType().replace("_", "~"));
				item.setValue(itemJson.getValue());
				item.setVisible(itemJson.isVisible() == null || itemJson.isVisible());
			}
			apuPart.getItems().add(item);
		}
	}

	private void processAttachments(List<Attachment> attchs, ApuEntity apuEntity, Map<String, Path> filesMap) {
		if (attchs != null) {
			int i = 0;
			for (Attachment attch : attchs) {
				ApuAttachment apuAttachment = new ApuAttachment();
				apuAttachment.setId(idService.getNextApuAttachmentId());
				apuAttachment.setName(attch.getName());
				apuAttachment.setOrder(++i);
				fileInputProcessor.processFile(attch.getFile(), DigitalObjectType.PUBLISHED, apuAttachment, null,
						filesMap);
				apuAttachment.setApu(apuEntity);
				apuEntity.getAttachments().add(apuAttachment);
			}
		}
	}

	private void recordRelations(ApuEntity apuEntity) {
		for (ApuPart part : apuEntity.getParts()) {
			for (ApuPartItem item : part.getItems()) {
				ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
				if (itemType != null && itemType.getType() == DataType.APU_REF) {
					relationsAddCache.add(new RelationKey(apuEntity.getUuid(), UUID.fromString(item.getValue()), item.getType()));
				}
			}
		}
	}

	private void flush(cz.aron.domain.ApuSource apuSource) {
		// persist apus first: managed daos (from existingDaos) may reference these still-transient
		// apus, and the relation query below triggers an auto-flush that would otherwise fail with
		// "references an unsaved transient instance"
		apuEntityRepository.saveAll(saveCache.values());
		apuEntityRepository.flush();

		// restore existing relations and store new
		var sources = relationsAddCache.stream().map(r -> r.source()).collect(Collectors.toSet());
		var existingRelations = relationRepository.findAllByApuSourceIdAndSourceIn(apuSource.getId(), sources);
		for (var existingRelation : existingRelations) {
			if (relationsAddCache.remove(new RelationKey(existingRelation.getSource(), existingRelation.getTarget(),
					existingRelation.getRelation()))) {
				// remove "remove mark" for existing relation
				existingRelation.setRemove(false);				
			}
			// reindex target
			apusToHaveIncomingRelsUpdated.add(existingRelation.getTarget());
		}
		// remaining relations are new
		var relationToAdd = relationsAddCache.stream().map(r -> {
			var rel = new Relation();
			rel.setId(idService.getNextRelationId());
			rel.setSource(r.source());
			rel.setTarget(r.target());
			rel.setRelation(r.relation());
			rel.setApuSource(apuSource);
			rel.setRemove(false);
			return rel;
		}).collect(Collectors.toList());
		relationRepository.saveAll(relationToAdd);
		relationsAddCache.clear();

		// Now to reindex all apus that reference these apus, to update labels in them
		List<UUID> updatedApusIds = saveCache.values().stream().map(ApuEntity::getUuid).collect(Collectors.toList());
		List<Long> apuIdsTargetingUpdatedIds = relationRepository.findIdsByTarget(updatedApusIds);
		// apuRepository.massIndex(apuIdsTargetingUpdatedIds);
		// clear for next batch
		indexingService.indexApus(saveCache.values(), apuService.resolveApuRefLabels(saveCache.values()));
		saveCache.clear();

		var relatedIncomingEntities = apuEntityRepository.findAllByIdIn(apuIdsTargetingUpdatedIds);
		indexingService.indexApus(relatedIncomingEntities, apuService.resolveApuRefLabels(relatedIncomingEntities));

		var relatedEntities = apuEntityRepository.findAllByUuidIn(apusToHaveIncomingRelsUpdated);
		indexingService.indexApus(relatedEntities, apuService.resolveApuRefLabels(relatedEntities));
		
		apusToHaveIncomingRelsUpdated.clear();
		entityManager.clear();
	}

	private void removeNotUsedRelations(cz.aron.domain.ApuSource apuSource) {
		var ids = relationRepository.findAllIdByApuSourceIdAndRemoveTrue(apuSource.getId());
		relationRepository.deleteAllById(ids);
		// TODO reindex referenced apus referenced from deleted		
	}

	private void fillDaoCache(List<String> daoIds) {
		existingDaos.clear();
		if (!daoIds.isEmpty()) {
			var daoUuids = daoIds.stream().map(UUID::fromString).collect(Collectors.toList());
			daoRepository.findAllByUuidIn(daoUuids).forEach(dao -> existingDaos.put(dao.getUuid().toString(), dao));
		}
	}

	record RelationKey(UUID source, UUID target, String relation) {
	}
	
    private class LevelStats {
    	private int depth;
    	private int pos;
    	private int childCnt = 0;
    	private boolean processed = false;
    }


}
