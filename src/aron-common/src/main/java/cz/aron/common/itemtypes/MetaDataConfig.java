package cz.aron.common.itemtypes;

public class MetaDataConfig {

    private final String code;
    private final String name;

    public MetaDataConfig(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}
