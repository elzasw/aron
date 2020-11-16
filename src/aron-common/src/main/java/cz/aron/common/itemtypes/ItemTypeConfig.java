package cz.aron.common.itemtypes;

public class ItemTypeConfig {

    private enum Types {
        APU_REF, INTEGER, STRING, UNITDATE, LINK
    }

    private final String code;
    private final String name;
    private final Types type;

    public ItemTypeConfig(String code, String name, Types type) {
        this.code = code;
        this.name = name;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Types getType() {
        return type;
    }

}
