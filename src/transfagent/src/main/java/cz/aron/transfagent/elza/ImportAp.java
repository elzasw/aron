package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;

public class ImportAp {
	ElzaXmlReader elzaXmlReader;	
	
	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
	
	static ApTypeService apTypeService = new ApTypeService();
	
	private ContextDataProvider dataProvider;
	
	Integer parentElzaId;

	private String apUuid;
	
	private Integer elzaId;
	
	private Set<Integer> requiredEntities = new HashSet<>();

	public String getApUuid() {
		return apUuid;
	}

	public ImportAp() {
		
	}
	
	public Set<Integer> getRequiredEntities() {
		return requiredEntities;
	}

	public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);
		ImportAp iap = new ImportAp();
		try {
			ApuSourceBuilder apusrcBuilder = iap.importAp(inputFile, args[1], args[2]);
			Path ouputPath = Paths.get(args[2]);
			try(OutputStream fos = Files.newOutputStream(ouputPath)) {
				apusrcBuilder.build(fos);
			}
		} catch(Exception e) {
			System.err.println("Failed to process input file: "+inputFile);
			e.printStackTrace();
		}

	}

	private ApuSourceBuilder importAp(Path inputFile, String expUuid, String propFile) throws IOException {
		Validate.isTrue(propFile!=null&&propFile.length()>0);
		
		PropertiesDataProvider pdp = new PropertiesDataProvider();
		Path propPath = Paths.get(propFile);
		pdp.load(propPath);
		return importAp(inputFile, expUuid, propFile);
	}

	public ApuSourceBuilder importAp(Path inputFile, String expUuid, final ContextDataProvider cdp) throws IOException, JAXBException {
		this.dataProvider = cdp;
		
		try(InputStream is = Files.newInputStream(inputFile);) {
			elzaXmlReader = ElzaXmlReader.read(is);
			return importAp(expUuid);
		}
	}

	private ApuSourceBuilder importAp(final String expUuid) {
		AccessPoint ap;
		if(expUuid!=null) {
			ap = elzaXmlReader.findAccessPointByUUID(expUuid);
			if(ap==null) {
				throw new IllegalStateException("AccessPoint not found: "+expUuid);
			}
		} else {
			// find first
			ap = elzaXmlReader.getSingleAccessPoint();
			if(ap==null) {
				throw new IllegalStateException("AccessPoint not found in result");
			}
		}
		this.apUuid = ap.getApe().getUuid();
		this.elzaId = Integer.valueOf(ap.getApe().getId());

		Apu apu = apusBuilder.createApu(null, ApuType.ENTITY);
		apu.setUuid(this.apUuid);
		
		// extrakce dat
		// entity info
		String entityClass = ap.getApe().getT();
		String ecName = apTypeService.getTypeName(entityClass);
		Validate.notNull(ecName, "Entity class name not found, code: %s", entityClass);
		Part aeInfoPart = apusBuilder.addPart(apu, CoreTypes.PT_AE_INFO);
		apusBuilder.addEnum(aeInfoPart, CoreTypes.AE_CLASS, ecName, true);
		
		String parentEcName = apTypeService.getParentName(entityClass);
		if(parentEcName!=null) {
			apusBuilder.addEnum(aeInfoPart, CoreTypes.AE_CLASS, parentEcName, false);
		}
		
		// oznaceni
		Fragments frgs = ap.getFrgs();		
		for(Fragment frg: frgs.getFrg()) {
			switch(frg.getT()) {
			case "PT_NAME":
				importName(apu, frg);
				break;
			case "PT_BODY":
				importBody(apu, aeInfoPart, frg);
				break;
			case "PT_IDENT":
			    importIdent(apu, frg);
			    break;
			case "PT_REL":
				
			default:
				throw new IllegalStateException("AP with unsupported part, type: "+frg.getT());
			}
		}
		if(apu.getName()==null) {
			throw new IllegalStateException("AP without name: "+apUuid);
		}
		
		return apusBuilder;
	}

	private void importIdent(Apu apu, Fragment frg) {
	    String identValue = ElzaXmlReader.getStringType(frg, ElzaTypes.IDN_VALUE);
	    if(StringUtils.isEmpty(identValue)) {
	        // skip empty idents
	        return;
	    }
        String identType = ElzaXmlReader.getEnumValue(frg, ElzaTypes.IDN_TYPE);
        switch(identType) {
        case "ISO3166_2":
        case "ISO3166_3":
        case "ISO3166_NUM":
        case "ISO3166_PART2":
        case "CZ_RETRO":
        case "TAXONOMY":        
        case "ORCID":
        case "PEVA":
        case "RUIAN":
        case "ARCHNUM":
            // ignored idents
            return;
        case "NUTSLAU":
            identType = "NUTS/LAU";
            break;
        default:
            throw new IllegalStateException("Unrecognized identifier: "+identType + ", value: "+identValue);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(identType).append(": ").append(identValue);
        // add ident
        Part part = apusBuilder.addPart(apu, "PT_IDENT");
        part.setValue(sb.toString());
        apusBuilder.addString(part, "AE_IDENT_TYPE", identType);
        apusBuilder.addString(part, "AE_IDENT_VALUE", identValue);
    }

    private void importBody(Apu apu, Part part, Fragment frg) {
		
		String briefDesc = ElzaXmlReader.getStringType(frg, ElzaTypes.BRIEF_DESC);
		if(StringUtils.isNotEmpty(briefDesc)) {
			apu.setDesc(briefDesc);
		}
		
		String adminPrntRefId = ElzaXmlReader.getApRef(frg, ElzaTypes.GEO_ADMIN_CLASS);
		if(StringUtils.isNotEmpty(adminPrntRefId)) {
			Validate.isTrue(this.parentElzaId==null);
			
			parentElzaId = Integer.valueOf(adminPrntRefId);
			
			String parentEntUuid = this.dataProvider.getArchivalEntityApuByElzaId(parentElzaId);
			if(parentEntUuid==null) {
			    this.requiredEntities.add(parentElzaId);
			} else {
			    this.apusBuilder.addApuRef(part, "AE_GEO_ADMIN_REF", parentEntUuid);
			}
			
		}
	}

	private void importName(Apu apu, Fragment frg) {
		String fullName = ElzaXmlReader.getFullName(frg);
		// add name
		if(apu.getName()==null) {
			apu.setName(fullName);
		}

		apusBuilder.addName(apu, fullName);
	}

	public Integer getParentElzaId() {
		return parentElzaId;
	}

	public Integer getElzaId() {
		return elzaId;
	}
}
