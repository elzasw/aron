package cz.aron.domain.facets.dto;

import java.util.ArrayList;
import java.util.List;

import cz.aron.domain.types.dto.LocalizedItem;

public class FacetConfigDto {
//    private WhenConfigDto when;
    private Object when;
    private FacetType type;
    private String source;
    private String title;
    private DisplayType display = DisplayType.ALWAYS;
    private int maxItems;
    private int displayedItems;
    private int maxDisplayedItems;
    private String tooltip;
    private String description;
    /**
     * Translations of the three display texts, filled from
     * searchConfig_localization.yaml; the values above are the source language
     * and stay the fallback.
     */
    private List<LocalizedItem> titleTranslations = new ArrayList<>();
    private List<LocalizedItem> tooltipTranslations = new ArrayList<>();
    private List<LocalizedItem> descriptionTranslations = new ArrayList<>();
    private List<TooltipSpec> tooltips;
    private String orderBy;
    private List<String> order = new ArrayList<>();
    private String group;
    private List<InnerFacet> facets = new ArrayList<>();
	public Object getWhen() {
		return when;
	}
	public void setWhen(Object when) {
		this.when = when;
	}
	public FacetType getType() {
		return type;
	}
	public void setType(FacetType type) {
		this.type = type;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public List<LocalizedItem> getTitleTranslations() {
		return titleTranslations;
	}

	public List<LocalizedItem> getTooltipTranslations() {
		return tooltipTranslations;
	}

	public List<LocalizedItem> getDescriptionTranslations() {
		return descriptionTranslations;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public DisplayType getDisplay() {
		return display;
	}
	public void setDisplay(DisplayType display) {
		this.display = display;
	}
	public int getMaxItems() {
		return maxItems;
	}
	public void setMaxItems(int maxItems) {
		this.maxItems = maxItems;
	}
	public int getDisplayedItems() {
		return displayedItems;
	}
	public void setDisplayedItems(int displayedItems) {
		this.displayedItems = displayedItems;
	}
	public int getMaxDisplayedItems() {
		return maxDisplayedItems;
	}
	public void setMaxDisplayedItems(int maxDisplayedItems) {
		this.maxDisplayedItems = maxDisplayedItems;
	}
	public String getTooltip() {
		return tooltip;
	}
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<TooltipSpec> getTooltips() {
		return tooltips;
	}
	public void setTooltips(List<TooltipSpec> tooltips) {
		this.tooltips = tooltips;
	}
	public String getOrderBy() {
		return orderBy;
	}
	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;
	}
	public List<String> getOrder() {
		return order;
	}
	public void setOrder(List<String> order) {
		this.order = order;
	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String group) {
		this.group = group;
	}
	public List<InnerFacet> getFacets() {
		return facets;
	}
	public void setFacets(List<InnerFacet> facets) {
		this.facets = facets;
	}    
    
}
