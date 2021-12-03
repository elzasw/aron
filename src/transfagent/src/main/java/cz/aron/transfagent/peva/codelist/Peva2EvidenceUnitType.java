package cz.aron.transfagent.peva.codelist;

public class Peva2EvidenceUnitType {
	
	private final String name;
	
	private final String mainEUTId;

	public Peva2EvidenceUnitType(String name, String mainEUTId) {
		this.name = name;
		this.mainEUTId = mainEUTId;
	}

	public String getName() {
		return name;
	}

	public String getMainEUTId() {
		return mainEUTId;
	}

}
