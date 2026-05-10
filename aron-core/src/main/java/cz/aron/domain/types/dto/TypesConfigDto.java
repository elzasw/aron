package cz.aron.domain.types.dto;

import java.util.ArrayList;
import java.util.List;

public class TypesConfigDto {
    private List<ApuPartType> partTypes = new ArrayList<>();
    private List<ItemType> itemTypes = new ArrayList<>();
    private List<MetadataType> metaDataTypes = new ArrayList<>();
    private List<ItemTypeGroup> itemGroups = new ArrayList<>();

    private Long currentCrc;
    private Long indexedFieldsCrc;
    
    public List<ApuPartType> getPartTypes() {
    	return partTypes;
    }

	public List<ItemType> getItemTypes() {
		return itemTypes;
	}

	public void setItemTypes(List<ItemType> itemTypes) {
		this.itemTypes = itemTypes;
	}

	public List<MetadataType> getMetaDataTypes() {
		return metaDataTypes;
	}

	public void setMetaDataTypes(List<MetadataType> metaDataTypes) {
		this.metaDataTypes = metaDataTypes;
	}

	public List<ItemTypeGroup> getItemGroups() {
		return itemGroups;
	}

	public void setItemGroups(List<ItemTypeGroup> itemGroups) {
		this.itemGroups = itemGroups;
	}

	public Long getCurrentCrc() {
		return currentCrc;
	}

	public void setCurrentCrc(Long currentCrc) {
		this.currentCrc = currentCrc;
	}

	public Long getIndexedFieldsCrc() {
		return indexedFieldsCrc;
	}

	public void setIndexedFieldsCrc(Long indexedFieldsCrc) {
		this.indexedFieldsCrc = indexedFieldsCrc;
	}

	public void setPartTypes(List<ApuPartType> partTypes) {
		this.partTypes = partTypes;
	}
}
