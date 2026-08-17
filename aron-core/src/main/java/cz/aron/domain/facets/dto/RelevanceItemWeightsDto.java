package cz.aron.domain.facets.dto;

/** One item type promoted above the allText baseline (the {@code items:} list). */
public class RelevanceItemWeightsDto {

	/** Item-type code as in the facet configuration (underscore or tilde form). */
	private String source;

	private Float phrase;

	private Float terms;

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
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
