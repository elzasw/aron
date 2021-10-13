package cz.aron.transfagent.config;

public class ConfigPeva2FundProperties {
	
    private boolean parseInternalChanges;
    
    // do popisu findu prida jazyky
    private boolean languages;
    
    // do popisu fondu prida evidencni jednotky
    private boolean evidenceUnits;
    
    // do popisu fondu prida mista puvodu
    private boolean placesOfOrigin;
    
    // do popisu fondu prida literaturu
    private boolean literature;
    
    // do popisu fondu prida k jakemu datu jsou udaje aktualni
    private boolean toDate;
    
    // do popisu fondu prida stav zachovani
    private boolean preservationStatus;
    
    // nazev fondu slozi z [nazevarchivu],[nazevfondu],[datace]
    private boolean composedFundName;
    
    // do popisu fondu v prehledu prida jako description popis puvodce
    private boolean originatorAsDescription;

	public boolean isParseInternalChanges() {
		return parseInternalChanges;
	}

	public void setParseInternalChanges(boolean parseInternalChanges) {
		this.parseInternalChanges = parseInternalChanges;
	}

	public boolean isLanguages() {
		return languages;
	}

	public void setLanguages(boolean languages) {
		this.languages = languages;
	}

	public boolean isEvidenceUnits() {
		return evidenceUnits;
	}

	public void setEvidenceUnits(boolean evidenceUnits) {
		this.evidenceUnits = evidenceUnits;
	}

	public boolean isPlacesOfOrigin() {
		return placesOfOrigin;
	}

	public void setPlacesOfOrigin(boolean placesOfOrigin) {
		this.placesOfOrigin = placesOfOrigin;
	}

	public boolean isLiterature() {
		return literature;
	}

	public void setLiterature(boolean literature) {
		this.literature = literature;
	}

	public boolean isToDate() {
		return toDate;
	}

	public void setToDate(boolean toDate) {
		this.toDate = toDate;
	}

	public boolean isPreservationStatus() {
		return preservationStatus;
	}

	public void setPreservationStatus(boolean preservationStatus) {
		this.preservationStatus = preservationStatus;
	}

	public boolean isComposedFundName() {
		return composedFundName;
	}

	public void setComposedFundName(boolean composedFundName) {
		this.composedFundName = composedFundName;
	}

	public boolean isOriginatorAsDescription() {
		return originatorAsDescription;
	}

	public void setOriginatorAsDescription(boolean originatorAsDescription) {
		this.originatorAsDescription = originatorAsDescription;
	}
	
}
