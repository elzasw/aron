package cz.aron.common.itemtypes;

import java.util.List;

public class ItemGroup {
    private String code;
    private List<String> items;
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public List<String> getItems() {
        return items;
    }
    public void setItems(List<String> items) {
        this.items = items;
    }
}
