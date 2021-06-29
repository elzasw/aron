package cz.aron.core.facets;

import cz.aron.core.facets.dto.FacetConfigDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@RestController
@RequestMapping("/facets")
public class FacetsApi {
    @Inject private FacetsLoader facetsLoader;

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
