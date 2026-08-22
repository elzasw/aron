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
import cz.aron.domain.facets.dto.TooltipSpec;
import cz.aron.domain.types.dto.LocalizedItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashSet;
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
            // checked here, while the source still reads as the file spells it, so
            // an error names something the operator can search for
            validate(facet);
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

    /**
     * What a facet says beyond its type and source. A mistake here used to be
     * either silent or an unhelpful SnakeYAML message; unknown keys the bean
     * binding already rejects, so what is left is the fields whose values it
     * cannot check.
     */
    private static void validate(FacetConfigDto facet) {
        // an unreadable condition would otherwise widen the facet's scope to every
        // section, which is invisible until a reader is offered somebody else's
        // facet (see FacetCondition)
        FacetCondition.parse(facet.getWhen(), facet.getSource());

        String orderBy = facet.getOrderBy();
        if (orderBy != null && !"ASC".equalsIgnoreCase(orderBy) && !"FREQ".equalsIgnoreCase(orderBy)) {
            // anything but ASC counts as FREQ, so a typo means "by frequency" -
            // silently the opposite of what an alphabetical order was asked for
            throw new IllegalStateException("searchConfig facet '" + facet.getSource()
                    + "': orderBy must be FREQ or ASC, not '" + orderBy + "'");
        }

        if (facet.getTooltips() != null) {
            var seen = new HashSet<String>();
            facet.getTooltips().stream()
                    .map(TooltipSpec::getValue)
                    .filter(value -> value != null && !seen.add(value))
                    // harmless - the first entry wins - but it means one of the two
                    // texts is never shown, which is rarely what was meant
                    .forEach(value -> log.warn(
                            "searchConfig facet {}: option '{}' has more than one tooltip; the first is used.",
                            facet.getSource(), value));
        }
    }

    /**
     * Adds the per-language display texts of one facet
     * ({@code language -> {title, tooltip, description, options}}), where
     * {@code options} maps an option's value to its own explanation.
     */
    private static void applyTranslations(FacetConfigDto facet, Map<String, Object> byLanguage) {
        if (byLanguage == null) {
            return;
        }
        byLanguage.forEach((language, texts) -> {
            if (!(texts instanceof Map<?, ?> fields)) {
                log.warn("searchConfig translations: {}/{} is not a mapping of title/tooltip/description/options"
                        + " - ignored.", language, facet.getSource());
                return;
            }
            add(facet.getTitleTranslations(), language, fields.get("title"));
            add(facet.getTooltipTranslations(), language, fields.get("tooltip"));
            add(facet.getDescriptionTranslations(), language, fields.get("description"));
            applyOptionTranslations(facet, language, fields.get("options"));
        });
    }

    /**
     * Translations of the per-option explanations, keyed by the option value the
     * facet's own {@code tooltips} entry names - the only stable key there is,
     * since an option has no code of its own.
     */
    private static void applyOptionTranslations(FacetConfigDto facet, String language, Object node) {
        if (node == null || facet.getTooltips() == null) {
            return;
        }
        if (!(node instanceof Map<?, ?> options)) {
            log.warn("searchConfig translations: {}/{} 'options' is not a mapping of value -> text - ignored.",
                    language, facet.getSource());
            return;
        }
        for (TooltipSpec spec : facet.getTooltips()) {
            Object text = options.get(spec.getValue());
            if (text != null) {
                add(spec.getTooltipTranslations(), language, text);
            }
        }
    }

    private static void add(List<LocalizedItem> translations, String language, Object text) {
        if (text instanceof String value && !value.isBlank()) {
            translations.add(new LocalizedItem(language, value));
        }
    }
}
