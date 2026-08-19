package cz.aron.search.relevance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.aron.domain.DataType;
import cz.aron.domain.facets.FacetsLoader;
import cz.aron.domain.facets.dto.RelevanceItemWeightsDto;
import cz.aron.domain.facets.dto.RelevanceSettingsDto;
import cz.aron.domain.types.TypesHolder;
import cz.aron.search.ContentLocale;
import cz.aron.domain.types.dto.ItemType;
import jakarta.annotation.PostConstruct;

/**
 * Resolves the deployment's relevance configuration at startup (searchConfig
 * yaml overlaid on the built-in defaults; logical item-type codes resolved to
 * physical fields - APU_REF sources score on their {@code ~LABEL} companion)
 * and plans fulltext queries with it. Weight changes need a restart, never a
 * reindex (doc/search-relevance.md §4.3).
 */
@Service
public class RelevanceService {

	private static final Logger log = LoggerFactory.getLogger(RelevanceService.class);

	private final FacetsLoader facetsLoader;

	private final TypesHolder typesHolder;

	/** Stop words are the described material's, so the query chains follow the content locale. */
	private final ContentLocale contentLocale;

	private RelevanceConfig config;

	public RelevanceService(FacetsLoader facetsLoader, TypesHolder typesHolder, ContentLocale contentLocale) {
		this.contentLocale = contentLocale;
		this.facetsLoader = facetsLoader;
		this.typesHolder = typesHolder;
	}

	@PostConstruct
	void load() {
		RelevanceSettingsDto settings;
		try {
			settings = facetsLoader.loadConfig().getRelevance();
		} catch (IOException e) {
			throw new UncheckedIOException("Fail to load search configuration", e);
		}

		// every APU_REF item type contributes its ~LABEL field to the
		// reference-labels tier
		var refLabelFields = new ArrayList<String>();
		for (ItemType itemType : typesHolder.getAllItemTypes()) {
			if (itemType.isIndexed() && itemType.getType() == DataType.APU_REF) {
				refLabelFields.add(itemType.getCode() + "~LABEL");
			}
		}

		var promotedFields = new ArrayList<RelevanceConfig.PromotedField>();
		if (settings != null) {
			for (RelevanceItemWeightsDto item : settings.getItems()) {
				ItemType itemType = typesHolder.getItemTypeForCode(item.getSource());
				if (itemType == null) {
					// report, do not silently ignore (startup validation of §4.3)
					log.warn("relevance.items: unknown item type '{}' - entry skipped.", item.getSource());
					continue;
				}
				String field = itemType.getType() == DataType.APU_REF
						? itemType.getCode() + "~LABEL"
						: itemType.getCode();
				promotedFields.add(new RelevanceConfig.PromotedField(field,
						item.getPhrase() != null ? item.getPhrase() : 0,
						item.getTerms() != null ? item.getTerms() : 0));
			}
		}

		config = RelevanceConfig.withSettings(settings, refLabelFields, promotedFields,
				QueryAnalyzers.of(contentLocale.getLocale()));
		log.info("Relevance configuration loaded: minimumShouldMatch={}%, relaxOnNoHits={}, "
				+ "refLabelFields={}, promotedFields={}.",
				config.minimumShouldMatchPercent(), config.relaxOnNoHits(),
				refLabelFields.size(), promotedFields.size());
	}

	/** Plans one fulltext query; {@code null} = nothing searchable (match all). */
	public RelevancePlan plan(String query) {
		RelevancePlan plan = RelevanceQueryPlanner.plan(query, config);
		if (plan != null && log.isDebugEnabled()) {
			log.debug("Planned query '{}': gate={} (minimumShouldMatch={}), scoring={}",
					query, plan.gate(), plan.minimumShouldMatch(), plan.scoring());
		}
		return plan;
	}

	public RelevanceConfig config() {
		return config;
	}

}
