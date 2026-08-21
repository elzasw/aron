package cz.aron.domain.facets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import cz.aron.commons.LocalizationFile;
import cz.aron.domain.facets.dto.FacetConfigDto;
import cz.aron.domain.facets.dto.FacetsConfigDto;
import cz.aron.domain.types.dto.LocalizedItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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
        return loadConfig().getFacets();
    }

    /** The whole searchConfig.yaml: facets plus the optional relevance section. */
    public FacetsConfigDto loadConfig() throws IOException {
        log.debug("Loading search configuration.");
        Yaml yaml = new Yaml();
        String yamlConfig = Files.readString(Paths.get(facetConfig), StandardCharsets.UTF_8);
        FacetsConfigDto facetsConfigDto = yaml.loadAs(yamlConfig, FacetsConfigDto.class);
        // translations are keyed by the source code as written here, so they are
        // applied before the underscores below become tildes
        var translations = LocalizationFile.byCode(LocalizationFile.besides(facetConfig), "facets");
        //we replace underscores with tildes because otherwise indexing would turn them to dots
        for (FacetConfigDto facet : facetsConfigDto.getFacets()) {
            applyTranslations(facet, translations.get(facet.getSource()));
            // the when-condition is checked here, while the source still reads as
            // the file spells it, so an error names something the operator can
            // search for; an unreadable condition fails the startup rather than
            // quietly widening the facet's scope (see FacetCondition)
            FacetCondition.parse(facet.getWhen(), facet.getSource());
            if (facet.getSource() != null) {
                facet.setSource(facet.getSource().replace("_", "~"));
            }
            if (facet.getGroup() != null) {
                facet.setGroup(facet.getGroup().replace("_", "~"));
            }
        }
        if (facetsConfigDto.getRelevance() != null) {
            for (var item : facetsConfigDto.getRelevance().getItems()) {
                if (item.getSource() != null) {
                    item.setSource(item.getSource().replace("_", "~"));
                }
            }
        }
        return facetsConfigDto;
    }

    /** Adds the per-language display texts of one facet ({@code language -> {title, tooltip, description}}). */
    private static void applyTranslations(FacetConfigDto facet, Map<String, Object> byLanguage) {
        if (byLanguage == null) {
            return;
        }
        byLanguage.forEach((language, texts) -> {
            if (!(texts instanceof Map<?, ?> fields)) {
                log.warn("searchConfig translations: {}/{} is not a mapping of title/tooltip/description - ignored.",
                        language, facet.getSource());
                return;
            }
            add(facet.getTitleTranslations(), language, fields.get("title"));
            add(facet.getTooltipTranslations(), language, fields.get("tooltip"));
            add(facet.getDescriptionTranslations(), language, fields.get("description"));
        });
    }

    private static void add(List<LocalizedItem> translations, String language, Object text) {
        if (text instanceof String value && !value.isBlank()) {
            translations.add(new LocalizedItem(language, value));
        }
    }
}
