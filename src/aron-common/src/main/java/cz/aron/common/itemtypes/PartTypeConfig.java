package cz.aron.common.itemtypes;

public class PartTypeConfig {

    private enum ViewTypes {
        STANDALONE, GROUPED
    }

    private final String code;
    private final String name;
    private final ViewTypes viewType;

    public PartTypeConfig(String code, String name, ViewTypes viewType) {
        this.code = code;
        this.name = name;
        this.viewType = viewType;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public ViewTypes getViewType() {
        return viewType;
    }

}
