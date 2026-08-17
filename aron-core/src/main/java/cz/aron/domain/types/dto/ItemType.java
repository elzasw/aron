package cz.aron.domain.types.dto;

import java.util.ArrayList;
import java.util.List;

import cz.aron.domain.DataType;

public class ItemType {
    private String code;
    private String name;
    private DataType type;
    private boolean indexed = true;
    /** Whether the item's value enters the general fulltext (allText); default true. */
    private Boolean fulltext;
    private Boolean indexFolding;
    private Boolean caseInsensitive;
    /**
     * Legacy key accepted from deployed types.yaml files and IGNORED - relevance
     * weights are query-side configuration in searchConfig.yaml (see
     * doc/search-relevance.md, R-1).
     */
    private Object indexBoost;
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
	public Object getIndexBoost() {
		return indexBoost;
	}
	public void setIndexBoost(Object indexBoost) {
		this.indexBoost = indexBoost;
	}
	public Boolean getFulltext() {
		return fulltext;
	}
	public void setFulltext(Boolean fulltext) {
		this.fulltext = fulltext;
	}
	/** Fulltext participation (allText): on unless explicitly disabled. */
	public boolean isFulltextEnabled() {
		return !Boolean.FALSE.equals(fulltext);
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
