package cz.aron.transfagent.transformation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class PropertiesDataProvider implements ContextDataProvider {

	Properties props = new Properties();

	public void load(Path propPath) throws IOException {
		try(InputStream is = Files.newInputStream(propPath);) {
			props.load(is);
		}		
	}

	@Override
	public String getInstitutionApu(String instCode) {
		String propName = "institution."+instCode;
		return getProperty(propName);
	}

	private String getProperty(String propName) {
		String apuUuid = props.getProperty(propName);
		if(apuUuid==null) {
			throw new RuntimeException("Mssing property: "+propName);
		}
		return apuUuid;
	}

	@Override
	public String getArchivalEntityApuByElzaId(Integer elzaId) {
		String propName = "entity."+elzaId;
		return getProperty(propName);
	}

	@Override
	public String getFundApu(String institutionCode, String fundCode) {
		String propName = "fund."+institutionCode+"."+fundCode;
		return getProperty(propName);
	}
}
