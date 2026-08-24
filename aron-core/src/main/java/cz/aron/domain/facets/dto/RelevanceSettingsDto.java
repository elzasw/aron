package cz.aron.domain.facets.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * The optional {@code relevance:} section of searchConfig.yaml - the query-side
 * ranking weights (doc/search-relevance.md §4.3). Every key is optional; the
 * built-in defaults apply to anything unset, so a deployment without the
 * section still ranks sensibly. Changes take effect after a restart, no
 * reindex.
 */
public class RelevanceSettingsDto {

	/** Minimum share of query tokens a document must match: {@code 100%}, {@code 75%}, ... */
	private String minimumShouldMatch;

	/** Retry with any-word matching when the strict query yields no hits. */
	private Boolean relaxOnNoHits;

	/**
	 * Inflection-aware matching where the content locale has a stemmer (R-17;
	 * Czech shipped). Default on; {@code false} disables the stemmed gate and
	 * tiers - query-side only, so toggling needs no reindex.
	 */
	private Boolean stemming;

	/**
	 * Minimum token length for automatic partial (substring) matching; shorter
	 * tokens must match a whole word. Default 3 (also the floor - the trigram
	 * size of the substring companions).
	 */
	private Integer partialMinLength;

	private RelevanceFieldWeightsDto name;

	/** Variant name forms (item types marked {@code nameVariant} in types.yaml). */
	private RelevanceFieldWeightsDto nameVariants;

	private RelevanceFieldWeightsDto refLabels;

	private RelevanceFieldWeightsDto description;

	private RelevanceFieldWeightsDto allText;

	/** Item types promoted above the allText baseline. */
	private List<RelevanceItemWeightsDto> items = new ArrayList<>();

	public String getMinimumShouldMatch() {
		return minimumShouldMatch;
	}

	public void setMinimumShouldMatch(String minimumShouldMatch) {
		this.minimumShouldMatch = minimumShouldMatch;
	}

	public Boolean getRelaxOnNoHits() {
		return relaxOnNoHits;
	}

	public void setRelaxOnNoHits(Boolean relaxOnNoHits) {
		this.relaxOnNoHits = relaxOnNoHits;
	}

	public Boolean getStemming() {
		return stemming;
	}

	public void setStemming(Boolean stemming) {
		this.stemming = stemming;
	}

	public Integer getPartialMinLength() {
		return partialMinLength;
	}

	public void setPartialMinLength(Integer partialMinLength) {
		this.partialMinLength = partialMinLength;
	}

	/** Legacy alias of {@link #setPartialMinLength} (the key's pre-substring name). */
	public void setPrefixMinLength(Integer prefixMinLength) {
		this.partialMinLength = prefixMinLength;
	}

	public RelevanceFieldWeightsDto getName() {
		return name;
	}

	public void setName(RelevanceFieldWeightsDto name) {
		this.name = name;
	}

	public RelevanceFieldWeightsDto getNameVariants() {
		return nameVariants;
	}

	public void setNameVariants(RelevanceFieldWeightsDto nameVariants) {
		this.nameVariants = nameVariants;
	}

	public RelevanceFieldWeightsDto getRefLabels() {
		return refLabels;
	}

	public void setRefLabels(RelevanceFieldWeightsDto refLabels) {
		this.refLabels = refLabels;
	}

	public RelevanceFieldWeightsDto getDescription() {
		return description;
	}

	public void setDescription(RelevanceFieldWeightsDto description) {
		this.description = description;
	}

	public RelevanceFieldWeightsDto getAllText() {
		return allText;
	}

	public void setAllText(RelevanceFieldWeightsDto allText) {
		this.allText = allText;
	}

	public List<RelevanceItemWeightsDto> getItems() {
		return items;
	}

	public void setItems(List<RelevanceItemWeightsDto> items) {
		this.items = items;
	}

}
