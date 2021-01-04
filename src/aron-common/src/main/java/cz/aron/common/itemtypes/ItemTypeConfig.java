package cz.aron.common.itemtypes;

public class ItemTypeConfig {

    public enum Types {
        ENUM, APU_REF, INTEGER, STRING, UNITDATE, LINK
    }

    private String code;
    private String name;
    private Types type;
    private boolean indexed;

    public ItemTypeConfig() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Types getType() {
        return type;
    }

    public void setType(Types type) {
        this.type = type;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

}
