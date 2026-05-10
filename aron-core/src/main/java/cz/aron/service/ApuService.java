package cz.aron.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterables;

import cz.aron.api.rest.model.ApuEntityTreeViewDto;
import cz.aron.api.rest.model.ApuEntityView;
import cz.aron.domain.ApuEntity;
import cz.aron.domain.ApuPart;
import cz.aron.domain.ApuPartItem;
import cz.aron.domain.DataType;
import cz.aron.domain.dto.IdLabelDto;
import cz.aron.domain.types.TypesHolder;
import cz.aron.domain.types.dto.ItemType;
import cz.aron.repository.ApuEntityRepository;

@Service
public class ApuService {

	private static final Logger log = LoggerFactory.getLogger(ApuService.class);

	private final TypesHolder typesHolder;

	private final ApuEntityRepository apuEntityRepository;
	    
    private int levelSize;

	public ApuService(TypesHolder typesHolder, ApuEntityRepository apuEntityRepository,
			@Value("${tree.levelSize:100}") int levelSize) {
		this.typesHolder = typesHolder;
		this.apuEntityRepository = apuEntityRepository;
		this.levelSize = levelSize;
	}

	private List<IdLabelDto> mapNames(Collection<String> ids) {
		var ret = new ArrayList<IdLabelDto>(ids.size());
		Iterables.partition(ids, 1000).forEach(partition -> ret.addAll(apuEntityRepository.listByUuids(partition)));
		return ret;
	}

	public void fillTargetLabelsToApuRefs(Collection<ApuEntity> apus) {
        //Find all referred ids
        var idsToFind = new HashSet<String>();
        for (ApuEntity apuEntity : apus) {
            for (ApuPart part : apuEntity.getParts()) {
                for (ApuPartItem item : part.getItems()) {
                    ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                    if (itemType == null) {
                        log.warn("unrecognized item type: " + item.getType());
                        continue;
                    }
                    if (itemType.getType() == DataType.APU_REF) {
                        idsToFind.add(item.getValue());
                    }
                }
            }
        }

        //Fetch their labels and put them to a map
        var idToLabelLookupMap = new HashMap<String,String>();
        for (IdLabelDto idLabelDto : mapNames(idsToFind)) {
            idToLabelLookupMap.put(idLabelDto.uuid(), idLabelDto.name());
        }
        //Use the map to fill labels to items
        for (ApuEntity apuEntity : apus) {
            for (ApuPart part : apuEntity.getParts()) {
                for (ApuPartItem item : part.getItems()) {
                    ItemType itemType = typesHolder.getItemTypeForCode(item.getType());
                    if (itemType == null) {
                        log.warn("unrecognized item type: " + item.getType());
                        continue;
                    }
                    if (itemType.getType() == DataType.APU_REF) {
                        item.setTargetLabel(idToLabelLookupMap.get(item.getValue()));
                    }
                }
            }
        }
    }

	@Transactional(readOnly=true)
	public List<ApuEntityTreeViewDto> getEntitiesBefore(String apuId) {
		var apu = apuEntityRepository.findByUuid(apuId);
		if (apu.getParent()!=null) {
			return apuEntityRepository.listEntitiesBefore(apu.getParent().getId(), apu.getPos(), levelSize);
		} else {
			return apuEntityRepository.listRootEntitiesBefore(apu.getSource().getId(), apu.getPos(), levelSize);
		}
	}

	@Transactional(readOnly=true)
	public List<ApuEntityTreeViewDto> getEntitiesAfter(String apuId) {
		var apu = apuEntityRepository.findByUuid(apuId);
		if (apu.getParent()!=null) {
			return apuEntityRepository.listEntitiesAfter(apu.getParent().getId(), apu.getPos(), levelSize);
		} else {
			return apuEntityRepository.listRootEntitiesAfter(apu.getSource().getId(), apu.getPos(), levelSize);
		}
	}

	@Transactional(readOnly = true)
	public List<ApuEntityTreeViewDto> getEntitiesUnder(String apuId) {
		var apu = apuEntityRepository.findByUuid(apuId);
		return apuEntityRepository.listEntitiesUnder(apu.getId(), levelSize);
	}

	@Transactional(readOnly = true)
	public List<ApuEntityView> findAllByUuids(List<String> ids) {
		var entities = apuEntityRepository.findAllByUuids(ids);
		var ret = new ArrayList<ApuEntityView>();
		for(var entity:entities) {
			ret.add(new ApuEntityView(entity.id(),entity.name(),entity.order()).description(entity.description()));
		}		
		return ret;
	}

}
