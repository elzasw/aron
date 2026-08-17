package cz.aron.domain.facets.dto;

/** Per-field weight overrides of the {@code relevance:} section (all optional). */
public class RelevanceFieldWeightsDto {

	private Float exactCs;

	private Float exact;

	private Float prefix;

	private Float phrase;

	private Float terms;

	public Float getExactCs() {
		return exactCs;
	}

	public void setExactCs(Float exactCs) {
		this.exactCs = exactCs;
	}

	public Float getExact() {
		return exact;
	}

	public void setExact(Float exact) {
		this.exact = exact;
	}

	public Float getPrefix() {
		return prefix;
	}

	public void setPrefix(Float prefix) {
		this.prefix = prefix;
	}

	public Float getPhrase() {
		return phrase;
	}

	public void setPhrase(Float phrase) {
		this.phrase = phrase;
	}

	public Float getTerms() {
		return terms;
	}

	public void setTerms(Float terms) {
		this.terms = terms;
	}

}
