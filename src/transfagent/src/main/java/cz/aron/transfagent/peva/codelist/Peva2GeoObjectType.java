package cz.aron.transfagent.peva.codelist;

public class Peva2GeoObjectType {
	
	private final String name;
	
	private final String oSubclass;
	
	public Peva2GeoObjectType(String name, String oSubclass) {
		this.name = name;
		this.oSubclass = oSubclass;
	}

	public String getName() {
		return name;
	}

	public String getoSubclass() {
		return oSubclass;
	}

}
