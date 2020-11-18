package cz.aron.common.itemtypes;

import java.util.List;

public class TypesConfiguration {

    private List<PartTypeConfig> partTypes;
    private List<ItemTypeConfig> itemTypes;
    private List<MetaDataConfig> metaDataTypes;

    public TypesConfiguration() {
    }

    public void setPartTypes(List<PartTypeConfig> partTypes) {
        this.partTypes = partTypes;
    }

    public void setItemTypes(List<ItemTypeConfig> itemTypes) {
        this.itemTypes = itemTypes;
    }

    public void setMetaDataTypes(List<MetaDataConfig> metaDataTypes) {
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
