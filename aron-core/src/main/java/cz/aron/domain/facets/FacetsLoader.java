package cz.aron.domain.facets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.facets.dto.FacetsConfigDto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class FacetsLoader {
	
	private static final Logger log = LoggerFactory.getLogger(FacetsLoader.class); 
	
    private final ResourceLoader resourceLoader;
    
    public FacetsLoader(ResourceLoader resourceLoader) {
    	this.resourceLoader = resourceLoader;
    }

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
