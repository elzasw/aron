package cz.aron.domain.types.dto;

import java.util.ArrayList;
import java.util.List;

public class ItemTypeGroup {
    private String code;
    private List<String> items = new ArrayList<>();
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
