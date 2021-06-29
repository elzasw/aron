package cz.aron.core.facets;

import cz.aron.core.facets.dto.FacetConfigDto;
import cz.aron.core.facets.dto.FacetsConfigDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 19.11.2020.
 */
@Service
@Slf4j
public class FacetsLoader {
    @Inject private ResourceLoader resourceLoader;

    @Value("${webResources.facets}")
    private String facetConfig;

    public List<FacetConfigDto> loadFacets() throws IOException {
        log.debug("Loading facets from config.");
        Yaml yaml = new Yaml();
        String yamlConfig = Files.readString(Paths.get(facetConfig), StandardCharsets.UTF_8);
        FacetsConfigDto facetsConfigDto = yaml.loadAs(yamlConfig, FacetsConfigDto.class);
        //we replace underscores with tildes because otherwise indexing would turn them to dots
        for (FacetConfigDto facet : facetsConfigDto.getFacets()) {
            if (facet.getSource() != null) {
                facet.setSource(facet.getSource().replace("_", "~"));
            }
            if (facet.getGroup() != null) {
                facet.setGroup(facet.getGroup().replace("_", "~"));
            }
        }
        return facetsConfigDto.getFacets();
    }
}
