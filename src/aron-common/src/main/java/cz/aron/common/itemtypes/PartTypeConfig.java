package cz.aron.common.itemtypes;

public class PartTypeConfig {

    private enum ViewTypes {
        STANDALONE, GROUPED
    }

    private String code;
    private String name;
    private ViewTypes viewType;

    public PartTypeConfig() {
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

    public ViewTypes getViewType() {
        return viewType;
    }

    public void setViewType(ViewTypes viewType) {
        this.viewType = viewType;
    }

}
