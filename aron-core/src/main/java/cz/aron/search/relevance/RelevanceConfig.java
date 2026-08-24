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
 * @param promotedFields            item fields promoted above the allText baseline
 * @param analyzers                 token chains of the described material's language
 */
public record RelevanceConfig(
		int minimumShouldMatchPercent,
		boolean relaxOnNoHits,
		int partialMinLength,
		float nameExact, float nameExactFolded, float namePrefix, float namePhrase, float nameTerms,
		float nameWordPrefix, float nameContains,
		float nameVariantsExact, float nameVariantsExactFolded, float nameVariantsPrefix,
		float nameVariantsPhrase, float nameVariantsTerms,
		float nameVariantsWordPrefix, float nameVariantsContains,
		float refLabelsPhrase, float refLabelsTerms,
		float descriptionPhrase, float descriptionTerms,
		float allTextTerms,
		List<PromotedField> promotedFields,
		QueryAnalyzers analyzers) {

	/** One promoted item field with its weights. */
	public record PromotedField(String field, float phrase, float terms) {
	}

	/** Built-in defaults with no promoted fields (tests, simple callers). */
	public static RelevanceConfig defaults() {
		return withSettings(null, List.of(), QueryAnalyzers.DEFAULT);
	}

	/**
	 * Built-in defaults overlaid with the deployment settings ({@code null} =
	 * pure defaults). {@code promotedFields} come resolved from the caller
	 * (item-type codes are a types.yaml concern).
	 */
	public static RelevanceConfig withSettings(RelevanceSettingsDto settings,
			List<PromotedField> promotedFields, QueryAnalyzers analyzers) {
		RelevanceFieldWeightsDto name = settings != null ? settings.getName() : null;
		RelevanceFieldWeightsDto nameVariants = settings != null ? settings.getNameVariants() : null;
		RelevanceFieldWeightsDto refLabels = settings != null ? settings.getRefLabels() : null;
		RelevanceFieldWeightsDto description = settings != null ? settings.getDescription() : null;
		RelevanceFieldWeightsDto allText = settings != null ? settings.getAllText() : null;
		return new RelevanceConfig(
				parseMinimumShouldMatch(settings != null ? settings.getMinimumShouldMatch() : null),
				settings == null || !Boolean.FALSE.equals(settings.getRelaxOnNoHits()),
				parsePartialMinLength(settings != null ? settings.getPartialMinLength() : null),
				weight(name != null ? name.getExact() : null, 1000),
				weight(name != null ? name.getExactFolded() : null, 800),
				weight(name != null ? name.getPrefix() : null, 200),
				weight(name != null ? name.getPhrase() : null, 100),
				weight(name != null ? name.getTerms() : null, 50),
				// partial words rank below every full-word tier (exact beats
				// partial), a word-start match above a mid-word one
				weight(name != null ? name.getWordPrefix() : null, 30),
				weight(name != null ? name.getContains() : null, 15),
				// preferred name ~ 5x a variant form (the CAM/Elza rule, see §2)
				weight(nameVariants != null ? nameVariants.getExact() : null, 200),
				weight(nameVariants != null ? nameVariants.getExactFolded() : null, 160),
				weight(nameVariants != null ? nameVariants.getPrefix() : null, 40),
				weight(nameVariants != null ? nameVariants.getPhrase() : null, 20),
				weight(nameVariants != null ? nameVariants.getTerms() : null, 10),
				weight(nameVariants != null ? nameVariants.getWordPrefix() : null, 8),
				weight(nameVariants != null ? nameVariants.getContains() : null, 4),
				weight(refLabels != null ? refLabels.getPhrase() : null, 12),
				weight(refLabels != null ? refLabels.getTerms() : null, 10),
				weight(description != null ? description.getPhrase() : null, 8),
				weight(description != null ? description.getTerms() : null, 2),
				weight(allText != null ? allText.getTerms() : null, 1),
				List.copyOf(promotedFields),
				analyzers);
	}

	private static float weight(Float configured, float defaultWeight) {
		return configured != null ? configured : defaultWeight;
	}

	/**
	 * Tokens at least this long match partially (substring); shorter must match
	 * whole. The floor is the trigram size of the {@code *Grams} companions -
	 * a shorter fragment yields no trigram and could never match.
	 */
	private static int parsePartialMinLength(Integer value) {
		if (value == null) {
			return 3;
		}
		if (value < 3) {
			throw new IllegalArgumentException("relevance.partialMinLength must be at least 3: " + value);
		}
		return value;
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
