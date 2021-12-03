package cz.aron.transfagent.peva.codelist;

public class Peva2OriginatorSubclass {

	private final String oClass;
	private final String name;
	private final String camCode;

	public Peva2OriginatorSubclass(String oClass, String name, String camCode) {
		this.oClass = oClass;
		this.name = name;
		this.camCode = camCode;
	}

	public String getoClass() {
		return oClass;
	}

	public String getName() {
		return name;
	}

	public String getCamCode() {
		return camCode;
	}
}
