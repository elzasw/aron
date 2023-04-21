package cz.aron.domain.facets.dto;

public class InnerFacet {
	private FacetType type;
	private String source;
	private int maxItems;

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

	public int getMaxItems() {
		return maxItems;
	}

	public void setMaxItems(int maxItems) {
		this.maxItems = maxItems;
	}
}
