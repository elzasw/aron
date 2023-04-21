package cz.aron.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.aron.domain.facets.FacetsLoader;
import cz.aron.domain.facets.dto.FacetConfigDto;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/facets")
public class FacetsApi {
    
	private final FacetsLoader facetsLoader;
	
	public FacetsApi(FacetsLoader facetsLoader) {
		this.facetsLoader = facetsLoader;
	}

    private List<FacetConfigDto> facets;

    @PostConstruct
    private void loadData() throws IOException {
        facets = facetsLoader.loadFacets();
    }

    @GetMapping
    public List<FacetConfigDto> getFacets() {
        return facets;
    }
}
