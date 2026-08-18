package cz.aron.domain.facets.dto;

/** Per-field weight overrides of the {@code relevance:} section (all optional). */
public class RelevanceFieldWeightsDto {

	private Float exact;

	private Float exactFolded;

	private Float prefix;

	private Float phrase;

	private Float terms;

	public Float getExact() {
		return exact;
	}

	public void setExact(Float exact) {
		this.exact = exact;
	}

	public Float getExactFolded() {
		return exactFolded;
	}

	public void setExactFolded(Float exactFolded) {
		this.exactFolded = exactFolded;
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
