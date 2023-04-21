package cz.aron.domain.types.dto;

import java.util.ArrayList;
import java.util.List;

import cz.aron.domain.DataType;

public class ItemType {
    private String code;
    private String name;
    private DataType type;
    private boolean indexed = true;
    private Boolean indexFolding;
    private Boolean caseInsensitive;
    private Boolean indexBoost;
    private List<LocalizedItem> lang = new ArrayList<>();
    private int viewOrder;
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
	public DataType getType() {
		return type;
	}
	public void setType(DataType type) {
		this.type = type;
	}
	public boolean isIndexed() {
		return indexed;
	}
	public void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}
	public Boolean getIndexFolding() {
		return indexFolding;
	}
	public void setIndexFolding(Boolean indexFolding) {
		this.indexFolding = indexFolding;
	}
	public Boolean getCaseInsensitive() {
		return caseInsensitive;
	}
	public void setCaseInsensitive(Boolean caseInsensitive) {
		this.caseInsensitive = caseInsensitive;
	}
	public Boolean getIndexBoost() {
		return indexBoost;
	}
	public void setIndexBoost(Boolean indexBoost) {
		this.indexBoost = indexBoost;
	}
	public List<LocalizedItem> getLang() {
		return lang;
	}
	public void setLang(List<LocalizedItem> lang) {
		this.lang = lang;
	}
	public int getViewOrder() {
		return viewOrder;
	}
	public void setViewOrder(int viewOrder) {
		this.viewOrder = viewOrder;
	}
    
    
    
    
}
