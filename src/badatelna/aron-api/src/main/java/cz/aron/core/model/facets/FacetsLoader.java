package cz.aron.core.model.facets;

import cz.aron.core.model.facets.dto.FacetConfigDto;
import cz.aron.core.model.facets.dto.FacetsConfigDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 19.11.2020.
 */
@Service
@Slf4j
public class FacetsLoader {
    @Inject private ResourceLoader resourceLoader;

    @Value("${facet-config}")
    private String facetConfig;

    public List<FacetConfigDto> loadFacets() {
        log.debug("Loading facets from config.");
        try (InputStream inputStream = resourceLoader.getResource(facetConfig).getInputStream()) {
            Yaml yaml = new Yaml();
            FacetsConfigDto facetsConfigDto = yaml.loadAs(inputStream, FacetsConfigDto.class);
            //we replace underscores with tildes because otherwise indexing would turn them to dots
            for (FacetConfigDto facet : facetsConfigDto.getFacets()) {
                facet.setSource(facet.getSource().replace("_", "~"));
            }
            return facetsConfigDto.getFacets();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
