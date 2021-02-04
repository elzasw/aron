package cz.aron.transfagent.transformation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class PropertiesDataProvider implements ContextDataProvider {

	Properties props = new Properties();

	public void load(Path propPath) throws IOException {
		try(InputStream is = Files.newInputStream(propPath);) {
			props.load(is);
		}		
	}

	@Override
	public UUID getInstitutionApu(String instCode) {
		String propName = "institution."+instCode;
		return UUID.fromString(getProperty(propName));
	}

	private String getProperty(String propName) {
		String apuUuid = props.getProperty(propName);
		if(apuUuid==null) {
			throw new RuntimeException("Mssing property: "+propName);
		}
		return apuUuid;
	}

	@Override
	public List<UUID> getArchivalEntityApuWithParentsByElzaId(Integer elzaId) {
		String propName = "entity."+elzaId;
		return List.of(UUID.fromString(getProperty(propName)));
	}

	@Override
	public UUID getFundApu(String institutionCode, String fundCode) {
		String propName = "fund."+institutionCode+"."+fundCode;
		return UUID.fromString(getProperty(propName));
	}

    @Override
    public List<UUID> findByUUIDWithParents(UUID apUuid) {
        return Collections.emptyList();
    }

    @Override
    public UUID getDao(String daoHandle) {
        return null;
    }
}
