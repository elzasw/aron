package cz.aron.domain.facets.dto;

import java.util.ArrayList;
import java.util.List;

public class FacetsConfigDto {
    private List<FacetConfigDto> facets = new ArrayList<>();

    /** Optional query-side relevance weights (doc/search-relevance.md §4.3). */
    private RelevanceSettingsDto relevance;

	public List<FacetConfigDto> getFacets() {
		return facets;
	}

	public void setFacets(List<FacetConfigDto> facets) {
		this.facets = facets;
	}

	public RelevanceSettingsDto getRelevance() {
		return relevance;
	}

	public void setRelevance(RelevanceSettingsDto relevance) {
		this.relevance = relevance;
	}
}
