package cz.aron.domain.facets.dto;

import java.util.ArrayList;
import java.util.List;

public class FacetsConfigDto {
    private List<FacetConfigDto> facets = new ArrayList<>();

	public List<FacetConfigDto> getFacets() {
		return facets;
	}

	public void setFacets(List<FacetConfigDto> facets) {
		this.facets = facets;
	}    
}
