package cz.aron.core.model;

import cz.aron.core.model.facets.FacetsHolder;
import cz.aron.core.model.facets.dto.FacetConfigDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@RestController
@RequestMapping("/facets")
public class FacetsApi {
    @Inject private FacetsHolder facetsHolder;

    @GetMapping
    public List<FacetConfigDto> list() {
        return facetsHolder.getFacets();
    }
}
