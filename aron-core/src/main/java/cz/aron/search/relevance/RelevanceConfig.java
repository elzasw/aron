package cz.aron.search.relevance;

import java.util.List;

import cz.aron.domain.facets.dto.RelevanceFieldWeightsDto;
import cz.aron.domain.facets.dto.RelevanceSettingsDto;

/**
 * Resolved relevance configuration (doc/search-relevance.md §4.2/§4.3):
 * built-in default weights overlaid with the deployment's {@code relevance:}
 * section of searchConfig.yaml. Field names are physical index fields -
 * logical item-type codes are resolved by the caller before construction.
 * A weight of 0 (or less) disables its tier.
 *
 * @param minimumShouldMatchPercent minimum share of query tokens a document
 *                                  must match (100 = all, the default)
 * @param relaxOnNoHits             retry any-word when the strict query has no hits
 * @param refLabelFields            physical {@code ~LABEL} fields of APU_REF item types
 * @param promotedFields            item fields promoted above the allText baseline
 */
public record RelevanceConfig(
		int minimumShouldMatchPercent,
		boolean relaxOnNoHits,
		float nameExactCs, float nameExact, float namePrefix, float namePhrase, float nameTerms,
		float refLabelsPhrase, float refLabelsTerms,
		float descriptionPhrase, float descriptionTerms,
		float allTextTerms,
		List<String> refLabelFields,
		List<PromotedField> promotedFields) {

	/** One promoted item field with its weights. */
	public record PromotedField(String field, float phrase, float terms) {
	}

	/** Built-in defaults with no reference-label or promoted fields (tests, simple callers). */
	public static RelevanceConfig defaults() {
		return withSettings(null, List.of(), List.of());
	}

	/**
	 * Built-in defaults overlaid with the deployment settings ({@code null} =
	 * pure defaults). {@code refLabelFields} and {@code promotedFields} come
	 * resolved from the caller (item-type codes are a types.yaml concern).
	 */
	public static RelevanceConfig withSettings(RelevanceSettingsDto settings, List<String> refLabelFields,
			List<PromotedField> promotedFields) {
		RelevanceFieldWeightsDto name = settings != null ? settings.getName() : null;
		RelevanceFieldWeightsDto refLabels = settings != null ? settings.getRefLabels() : null;
		RelevanceFieldWeightsDto description = settings != null ? settings.getDescription() : null;
		RelevanceFieldWeightsDto allText = settings != null ? settings.getAllText() : null;
		return new RelevanceConfig(
				parseMinimumShouldMatch(settings != null ? settings.getMinimumShouldMatch() : null),
				settings == null || !Boolean.FALSE.equals(settings.getRelaxOnNoHits()),
				weight(name != null ? name.getExactCs() : null, 1000),
				weight(name != null ? name.getExact() : null, 800),
				weight(name != null ? name.getPrefix() : null, 200),
				weight(name != null ? name.getPhrase() : null, 100),
				weight(name != null ? name.getTerms() : null, 50),
				weight(refLabels != null ? refLabels.getPhrase() : null, 12),
				weight(refLabels != null ? refLabels.getTerms() : null, 10),
				weight(description != null ? description.getPhrase() : null, 8),
				weight(description != null ? description.getTerms() : null, 2),
				weight(allText != null ? allText.getTerms() : null, 1),
				List.copyOf(refLabelFields),
				List.copyOf(promotedFields));
	}

	private static float weight(Float configured, float defaultWeight) {
		return configured != null ? configured : defaultWeight;
	}

	/** Accepts {@code 75%}, {@code 75} or unset (= 100). */
	private static int parseMinimumShouldMatch(String value) {
		if (value == null || value.isBlank()) {
			return 100;
		}
		String number = value.trim();
		if (number.endsWith("%")) {
			number = number.substring(0, number.length() - 1).trim();
		}
		int percent = Integer.parseInt(number);
		if (percent < 1 || percent > 100) {
			throw new IllegalArgumentException("relevance.minimumShouldMatch must be within 1-100%: " + value);
		}
		return percent;
	}

}
