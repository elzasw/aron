package cz.aron.core.model.types.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 18.11.2020.
 */
@Getter
@Setter
public class TypesConfigDto {
    private List<ApuPartType> partTypes = new ArrayList<>();
    private List<ItemType> itemTypes = new ArrayList<>();
    private List<MetadataType> metaDataTypes = new ArrayList<>();
    private List<ItemTypeGroup> itemGroups = new ArrayList<>();

    private Long currentCrc;
}
