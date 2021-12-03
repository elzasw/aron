package cz.aron.transfagent.peva.codelist;

public class Peva2DatingMethod {
	
	private final String name;
	private final String type;
	private final String camCode;

	public Peva2DatingMethod(String name, String type, String camCode) {
		this.name = name;
		this.type = type;
		this.camCode = camCode;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getCamCode() {
		return camCode;
	}

}
