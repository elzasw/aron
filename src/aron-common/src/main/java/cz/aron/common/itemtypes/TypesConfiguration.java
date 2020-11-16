package cz.aron.common.itemtypes;

import java.util.List;

public class TypesConfiguration {

    private final List<PartTypeConfig> partTypes;
    private final List<ItemTypeConfig> itemTypes;
    private final List<MetaDataConfig> metaDataTypes;

    public TypesConfiguration(List<PartTypeConfig> partTypes, List<ItemTypeConfig> itemTypes,
                              List<MetaDataConfig> metaDataTypes) {
        this.partTypes = partTypes;
        this.itemTypes = itemTypes;
        this.metaDataTypes = metaDataTypes;
    }

    public List<PartTypeConfig> getPartTypes() {
        return partTypes;
    }

    public List<ItemTypeConfig> getItemTypes() {
        return itemTypes;
    }

    public List<MetaDataConfig> getMetaDataTypes() {
        return metaDataTypes;
    }

}
