package cz.aron.core.model.types;

import cz.aron.core.model.types.dto.*;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.*;

/**
 * @author Lukas Jane (inQool) 19.11.2020.
 */
@Service
public class TypesHolder {
    @Inject private TypesLoader typesLoader;

    private Map<String, ApuPartType> apuPartTypeMap = new LinkedHashMap<>();
    private Map<String, MetadataType> metadataTypeMap = new LinkedHashMap<>();
    private Map<String, ItemType> itemTypeMap = new LinkedHashMap<>();
    private Map<String, List<String>> itemTypeToItemGroupMap = new LinkedHashMap<>();

    @Getter
    private Long currentConfigCrc;

    @PostConstruct
    private void loadData() {
        TypesConfigDto typesConfigDto = typesLoader.loadTypes();
        for (ApuPartType apuPartType : typesConfigDto.getPartTypes()) {
            apuPartTypeMap.put(apuPartType.getCode(), apuPartType);
        }
        for (ItemType itemType : typesConfigDto.getItemTypes()) {
            itemTypeMap.put(itemType.getCode(), itemType);
        }
        for (MetadataType metadataType : typesConfigDto.getMetaDataTypes()) {
            metadataTypeMap.put(metadataType.getCode(), metadataType);
        }
        for (ItemTypeGroup itemTypeGroup : typesConfigDto.getItemGroups()) {
            for (String itemType : itemTypeGroup.getItems()) {
                itemTypeToItemGroupMap.computeIfAbsent(itemType, k -> new ArrayList<>()).add(itemTypeGroup.getCode());
            }
        }
        currentConfigCrc = typesConfigDto.getCurrentCrc();
    }

    public Collection<ApuPartType> getAllApuPartTypes() {
        return apuPartTypeMap.values();
    }

    public Collection<ItemType> getAllItemTypes() {
        return itemTypeMap.values();
    }

    public Collection<MetadataType> getAllMetadataTypes() {
        return metadataTypeMap.values();
    }

    public ApuPartType getApuPartTypeForCode(String code) {
        return apuPartTypeMap.get(code);
    }

    public ItemType getItemTypeForCode(String code) {
        return itemTypeMap.get(code);
    }

    public MetadataType getMetadataTypeForCode(String code) {
        return metadataTypeMap.get(code);
    }

    public List<String> getItemGroupsForItemType(String itemType) {
        List<String> itemGroups = itemTypeToItemGroupMap.get(itemType);
        if (itemGroups == null) {
            itemGroups = new ArrayList<>();
        }
        return itemGroups;
    }
}
