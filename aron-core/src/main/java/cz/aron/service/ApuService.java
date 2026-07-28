package cz.aron.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.google.common.collect.Iterables;

import cz.aron.api.rest.model.ApuEntityTreeViewDto;
import cz.aron.api.rest.model.ApuEntityView;
import cz.aron.api.rest.model.ApuPart;
import cz.aron.api.rest.model.ApuPartItem;
import cz.aron.commons.HttpUtils;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.DataType;
import cz.aron.domain.dto.IdLabelDto;
import cz.aron.domain.dto.IdUuidNameDescriptionParentDto;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.mapper.ApuEntityMapper;
import cz.aron.repository.ApuEntityRepository;

@Service
public class ApuService {

	private static final Logger log = LoggerFactory.getLogger(ApuService.class);

	private final TypesHolder typesHolder;

	private final ApuEntityRepository apuEntityRepository;
	
	private final ApuEntityMapper apuEntityMapper;
	    
    private int levelSize;

	public ApuService(TypesHolder typesHolder, ApuEntityRepository apuEntityRepository, ApuEntityMapper apuEntityMapper,
			@Value("${tree.levelSize:100}") int levelSize) {
		this.typesHolder = typesHolder;
		this.apuEntityRepository = apuEntityRepository;
		this.apuEntityMapper = apuEntityMapper;
		this.levelSize = levelSize;
	}

	private List<IdLabelDto> mapNames(Collection<UUID> ids) {
		var ret = new ArrayList<IdLabelDto>(ids.size());
		Iterables.partition(ids, 1000).forEach(partition -> ret.addAll(apuEntityRepository.listByUuids(partition)));
		return ret;
	}

	/**
	 * Resolves the display/indexed labels for all APU_REF items referenced by the given APUs.
	 * The result is a runtime-only lookup (keyed by referenced APU uuid) consumed by
	 * {@code IndexingService} — the labels are neither serialized into the parts blob nor exposed
	 * through the REST model.
	 */
	public Map<String, IdLabelDto> resolveApuRefLabels(Collection<ApuEntity> apus) {
        //Find all referred ids (APU_REF values are uuid strings in the serialized parts)
        var idsToFind = new HashSet<UUID>();
        for (ApuEntity apuEntity : apus) {
            for (ApuPart part : apuEntity.getParts()) {
                for (ApuPartItem item : part.getItems()) {
                    ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                    if (itemType == null) {
                        log.warn("unrecognized item type: " + item.getType());
                        continue;
                    }
                    if (itemType.getType() == DataType.APU_REF) {
                        idsToFind.add(UUID.fromString(item.getValue()));
                    }
                }
            }
        }

        //Fetch their labels and put them to a map keyed by the uuid string (matches item.getValue())
        var idToLabelLookupMap = new HashMap<String,IdLabelDto>();
        for (IdLabelDto idLabelDto : mapNames(idsToFind)) {
            idToLabelLookupMap.put(idLabelDto.uuid().toString(), idLabelDto);
        }
        return idToLabelLookupMap;
    }

	/**
	 * Returns all ancestor APUs of the given APU, from its immediate parent up to the root,
	 * ordered nearest-first. The APU identified by {@code id} itself is excluded.
	 */
	@Transactional(readOnly = true)
	public List<IdUuidNameDescriptionParentDto> getAncestorsToRoot(long id) {
		return apuEntityRepository.findAncestors(id);
	}

	@Transactional(readOnly=true)
	public Result<List<ApuEntityTreeViewDto>> getEntitiesBefore(UUID apuId, String ifNoneMatch, String ifModifiedSince) {
		var apu = apuEntityRepository.findByUuid(apuId);
		if (apu == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such APU.");
		}
		var expireStatus = HttpUtils.computeExpired(apu.getSource().getPublished(), ifNoneMatch, ifModifiedSince);
		if (!expireStatus.expired()) {
			return new NotModified<>(expireStatus.eTag(), expireStatus.lastModified());
		}
		List<ApuEntityTreeViewDto> result;
		if (apu.getParent()!=null) {
			result = apuEntityRepository.listEntitiesBefore(apu.getParent().getId(), apu.getPos(), levelSize);
		} else {
			result = apuEntityRepository.listRootEntitiesBefore(apu.getSource().getId(), apu.getPos(), levelSize);
		}
		// queries order by pos desc to grab the nearest items; reverse to restore ascending tree order
		Collections.reverse(result);
		return new Data<>(result, expireStatus.eTag(), expireStatus.lastModified());
	}

	@Transactional(readOnly=true)
	public Result<List<ApuEntityTreeViewDto>> getEntitiesAfter(UUID apuId, String ifNoneMatch, String ifModifiedSince) {
		var apu = apuEntityRepository.findByUuid(apuId);
		if (apu == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such APU.");
		}
		var expireStatus = HttpUtils.computeExpired(apu.getSource().getPublished(), ifNoneMatch, ifModifiedSince);
		if (!expireStatus.expired()) {
			return new NotModified<>(expireStatus.eTag(), expireStatus.lastModified());
		}
		List<ApuEntityTreeViewDto> result;
		if (apu.getParent()!=null) {
			result = apuEntityRepository.listEntitiesAfter(apu.getParent().getId(), apu.getPos(), levelSize);
		} else {
			result = apuEntityRepository.listRootEntitiesAfter(apu.getSource().getId(), apu.getPos(), levelSize);
		}
		return new Data<>(result, expireStatus.eTag(), expireStatus.lastModified());
	}

	@Transactional(readOnly = true)
	public Result<List<ApuEntityTreeViewDto>> getEntitiesUnder(UUID apuId, String ifNoneMatch, String ifModifiedSince) {
		var apu = apuEntityRepository.findByUuid(apuId);
		if (apu == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such APU.");
		}
		var expireStatus = HttpUtils.computeExpired(apu.getSource().getPublished(), ifNoneMatch, ifModifiedSince);
		if (!expireStatus.expired()) {
			return new NotModified<>(expireStatus.eTag(), expireStatus.lastModified());
		}
		var result = apuEntityRepository.listEntitiesUnder(apu.getId(), levelSize);
		return new Data<>(result, expireStatus.eTag(), expireStatus.lastModified());
	}

	/**
	 * The moment the given APU was last published — the later of its source's publish time and,
	 * when present, the time a DAO was connected to it. Used as the {@code Last-Modified}/ETag basis.
	 */
	private LocalDateTime publishedOf(ApuEntity apu) {
		var published = apu.getSource().getPublished();
		if (apu.getDaoPublished() != null && published.isBefore(apu.getDaoPublished())) {
			published = apu.getDaoPublished();
		}
		return published;
	}

	@Transactional(readOnly = true)
	public List<ApuEntityView> findAllByUuids(List<UUID> ids) {
		var entities = apuEntityRepository.findAllByUuids(ids);
		var ret = new ArrayList<ApuEntityView>();
		for(var entity:entities) {
			ret.add(new ApuEntityView(entity.id(),entity.name(),entity.order()).description(entity.description()));
		}		
		return ret;
	}

	@Transactional(readOnly = true)
	public Result<cz.aron.api.rest.model.ApuEntity> getApuEntity(UUID apuId, String ifNoneMatch, String ifModifiedSince) {
		var src = apuEntityRepository.findByUuid(apuId);
		if (src == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such APU.");
		}

		var expireStatus = HttpUtils.computeExpired(publishedOf(src), ifNoneMatch, ifModifiedSince);
		if (!expireStatus.expired()) {
			return new NotModified<>(expireStatus.eTag(), expireStatus.lastModified());
		}

		cz.aron.api.rest.model.ApuEntity dto = new cz.aron.api.rest.model.ApuEntity();
		dto.setId(apuId.toString());
        dto.setName(src.getName());
        dto.setDescription(src.getDescription());
        dto.setPermalink(src.getPermalink());
        dto.setOrder(src.getOrder());
        dto.setPublished(src.isPublished());
        dto.setType(ApuEntityMapper.toApuTypeEnum(src.getType()));
        dto.setPos(src.getPos());
        dto.setDepth(src.getDepth());
        dto.setChildCnt(src.getChildCnt());

        // parts are stored (and deserialized) directly as the REST model
        dto.setParts(src.getParts());
		if (src.getParent()!=null) {
			var ancestors = apuEntityRepository.findAncestors(src.getId());		
			cz.aron.api.rest.model.ApuEntity current = dto;
			for(var ancestor: ancestors) {
				cz.aron.api.rest.model.ApuEntity ancestorDto = new cz.aron.api.rest.model.ApuEntity();
				ancestorDto.setId(ancestor.uuid().toString());
				ancestorDto.setName(ancestor.name());
				ancestorDto.setDescription(ancestor.description());
				ancestorDto.setChildCnt(ancestor.childCnt());
				ancestorDto.setDepth(ancestor.depth());
				ancestorDto.setOrder(ancestor.ordr());
				ancestorDto.setPos(ancestor.pos());
				current.setParent(ancestorDto);
				current = ancestorDto;
			}
		}

		// attachments
		if (src.isHasAttachments()) {
			dto.setAttachments(apuEntityMapper.toRestAttachments(src.getAttachments()));
		}

		// daos
		if (src.hasDaos()) {
			dto.setDigitalObjects(apuEntityMapper.toRestDigitalObjects(src.getDigitalObjects()));
		}

		return new Data<>(dto, expireStatus.eTag(), expireStatus.lastModified());
	}
	
	public sealed interface Result<T> permits Data, NotModified {
	}

	public record Data<T>(T value, String eTag, long lastModified) implements Result<T> {
	}
	
	public record NotModified<T>(String eTag, long lastModified) implements Result<T> {
	}

}
