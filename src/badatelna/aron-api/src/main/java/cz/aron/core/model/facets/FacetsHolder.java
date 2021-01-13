package cz.aron.core.model.facets;

import cz.aron.core.model.facets.dto.FacetConfigDto;
import lombok.Getter;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 19.11.2020.
 */
@Service
public class FacetsHolder {
    @Inject private FacetsLoader facetsLoader;

    @Getter
    private List<FacetConfigDto> facets;

    @PostConstruct
    private void loadData() {
        facets = facetsLoader.loadFacets();
    }
}
