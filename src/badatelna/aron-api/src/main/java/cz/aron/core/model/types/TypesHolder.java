package cz.aron.core.model.types;

import cz.aron.core.model.types.dto.ApuPartType;
import cz.aron.core.model.types.dto.ItemType;
import cz.aron.core.model.types.dto.MetadataType;
import cz.aron.core.model.types.dto.TypesConfigDto;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Lukas Jane (inQool) 19.11.2020.
 */
@Service
public class TypesHolder {
    @Inject private TypesLoader typesLoader;

    private Map<String, ApuPartType> apuPartTypeMap = new LinkedHashMap<>();
    private Map<String, MetadataType> metadataTypeMap = new LinkedHashMap<>();
    private Map<String, ItemType> itemTypeMap = new LinkedHashMap<>();

    @PostConstruct
    private void loadData() {
        TypesConfigDto typesConfigDto = typesLoader.loadTypes();
        for (ApuPartType apuPartType : typesConfigDto.getPartyTypes()) {
            apuPartTypeMap.put(apuPartType.getCode(), apuPartType);
        }
        for (ItemType itemType : typesConfigDto.getItemTypes()) {
            itemTypeMap.put(itemType.getCode(), itemType);
        }
        for (MetadataType metadataType : typesConfigDto.getMetaDataTypes()) {
            metadataTypeMap.put(metadataType.getCode(), metadataType);
        }
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
}
