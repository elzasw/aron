package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.Validate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.convertor.EdxApRefConvertor;
import cz.aron.transfagent.elza.convertor.EdxApRefWithRole;
import cz.aron.transfagent.elza.convertor.EdxEnumConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemCovertContext;
import cz.aron.transfagent.elza.convertor.EdxNullConvertor;
import cz.aron.transfagent.elza.convertor.EdxStringConvertor;
import cz.aron.transfagent.elza.convertor.EdxTimeLenghtConvertor;
import cz.aron.transfagent.elza.convertor.EdxUnitDateConvertor;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;
import cz.tacr.elza.schema.v2.DigitalArchivalObject;
import cz.tacr.elza.schema.v2.DigitalArchivalObjects;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Levels;
import cz.tacr.elza.schema.v2.Section;
import cz.tacr.elza.schema.v2.Sections;

public class ImportArchDesc implements EdxItemCovertContext {

	ElzaXmlReader elzaXmlReader;	

	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

	ContextDataProvider dataProvider;

	Map<Apu, Apu> apuParentMap = new HashMap<>();

	final Set<UUID> apRefs = new HashSet<>();

	private Part activePart;

	private String institutionCode;

	private Apu activeApu; 
	
	public static void main(String[] args) {
		Path inputFile = Path.of(args[0]);
		ImportArchDesc iad = new ImportArchDesc();
		try {
			ApuSourceBuilder apusrcBuilder = iad.importArchDesc(inputFile, args[1]);
			Path ouputPath = Paths.get(args[2]);
			try(OutputStream fos = Files.newOutputStream(ouputPath)) {
				apusrcBuilder.build(fos);
			}
		} catch(Exception e) {
			System.err.println("Failed to process input file: "+inputFile);
			e.printStackTrace();
		}
	}
	
	public ImportArchDesc() {
	}

	public Set<UUID> getApRefs() {
		return apRefs;
	}

	public String getInstitutionCode() {
		return institutionCode;
	}

	private ApuSourceBuilder importArchDesc(Path inputFile, String propFile) throws IOException, JAXBException {
		Validate.isTrue(propFile!=null&&propFile.length()>0);
		
		PropertiesDataProvider pdp = new PropertiesDataProvider();
		Path propPath = Paths.get(propFile);
		pdp.load(propPath);
		return importArchDesc(inputFile, pdp);
	}

    public ApuSourceBuilder importArchDesc(Path inputFile,
											final ContextDataProvider cdp) throws IOException, JAXBException {
		this.dataProvider = cdp;
		
		try(InputStream is = Files.newInputStream(inputFile);) {
			elzaXmlReader = ElzaXmlReader.read(is);
			return importArchDesc();
		}		
	}

	private ApuSourceBuilder importArchDesc() {
		Sections sections = elzaXmlReader.getEdx().getFs();
		if(sections==null||sections.getS().size()==0) {
			throw new RuntimeException("Missing section data");
		}
		if(sections.getS().size()>1) {
			throw new RuntimeException("Exports with one section are supported");
		}
		Section sect = sections.getS().get(0);
		if(sect.getLvls()==null) {
			throw new RuntimeException("Missing levels.");
		}

		// read fund info
		FundInfo fi = sect.getFi();
		institutionCode = fi.getIc();
		var instApu = dataProvider.getInstitutionApu(institutionCode);
		Validate.notNull(instApu, "Missing institution, code: %s", institutionCode);

		var fundApu = dataProvider.getFundApu(institutionCode, fi.getC());
		Validate.notNull(fundApu, "Missing fund, code: %s, institution: %s", fi.getC(), institutionCode);

		Map<String, Apu> apuMap = new HashMap<>();

		Levels lvls = sect.getLvls();
		for(Level lvl: lvls.getLvl()) {
			String name = getName(sect, lvl);
			Apu apu = apusBuilder.createApu(name, ApuType.ARCH_DESC);
			apu.setUuid(lvl.getUuid());
			// set parent
			if(lvl.getPid()!=null) {
				Apu parentApu = apuMap.get(lvl.getPid());
				if(parentApu==null) {
					throw new RuntimeException("Missing parent for level: "+lvl.getPid());
				}
				apu.setPrnt(parentApu.getUuid());
				apuParentMap.put(apu,  parentApu);
			}
			apuMap.put(lvl.getId(), apu);
			
			activateArchDescPart(apu);
			// add items	
			for(DescriptionItem item: lvl.getDdOrDoOrDp()) {
				addItem(apu, item);
			}
			
			// daos
			DigitalArchivalObjects daos = lvl.getDaos();
			if(daos!=null&&daos.getDao().size()>0) {
				apusBuilder.addEnum(activePart, "DIGITAL", "Ano", false);
				for(DigitalArchivalObject dao: daos.getDao()) {
					apusBuilder.addDao(activeApu, dao.getId());
				}
			}
			deactivatePart(apu);
			
			// Add static values
			activatePart(apu, CoreTypes.PT_ARCH_DESC_FUND);
            apusBuilder.addApuRef(activePart, "FUND_REF", fundApu);
            apusBuilder.addApuRef(activePart, "FUND_INST_REF", instApu);
            deactivatePart(apu);
		}		
		return apusBuilder;
	}
	
	private void deactivatePart(Apu apu) {
        if(activePart!=null) {
            // delete empty part
            if(activePart.getItms()==null&&activePart.getValue()==null) {
               apu.getPrts().getPart().remove(activePart); 
            }
            activePart = null;
        }
        
    }

    /**
	 * Set PT_ARCH_DESC as active part
	 */
	void activateArchDescPart(Apu apu) {
	    activatePart(apu, CoreTypes.PT_ARCH_DESC);
	}

    void activatePart(Apu apu, String partName) {        
        activeApu = apu;
        activePart = null;
        if(apu.getPrts()!=null) {
            for (Part p : apu.getPrts().getPart()) {
                if (p.getType().equals(partName)) {
                    activePart = p;
                    break;
                }
            }
        }
        if(activePart==null) {
            activePart = apusBuilder.addPart(apu, partName);
        }       
    }

    private void addItem(Apu apu, DescriptionItem item) {
        if(item instanceof DescriptionItemUndefined ) {
            return;
        }
        
		final String ignoredTypes[] = {"ZP2015_ARRANGEMENT_TYPE", "ZP2015_LEVEL_TYPE",
				"ZP2015_SERIAL_NUMBER", "ZP2015_NAD", "ZP2015_ZNACKA_FONDU",
				"ZP2015_LEVEL_TYPE", "ZP2015_ARRANGER",
				"ZP2015_UNIT_DATE_BULK","ZP2015_FOLDER_TYPE",
				"ZP2015_STORAGE_ID", "ZP2015_ITEM_ORDER",
				"ZP2015_UNIT_COUNT_ITEM",
				"ZP2015_INTERNAL_NOTE",
				// geo souradnice - neumime prevest
				ElzaTypes.ZP2015_POSITION,
		// TODO: k zapracovani				
				"ZP2015_DATE_OTHER"
		};
		
		// check ignored items
		for(String ignoredType: ignoredTypes) {
			if(ignoredType.equals(item.getT())) {
				return;
			}
		}
		
		Map<String, EdxItemConvertor> stringTypeMap = new HashMap<>();
		stringTypeMap.put("ZP2015_TITLE",new EdxStringConvertor("ABSTRACT"));
		stringTypeMap.put("ZP2015_UNIT_TYPE", new EdxEnumConvertor("UNIT_TYPE", ElzaTypes.unitTypeMap));
		stringTypeMap.put("ZP2015_UNIT_ID",new EdxStringConvertor("UNIT_ID"));
		stringTypeMap.put("ZP2015_UNIT_HIST",new EdxStringConvertor("HISTORY"));
		stringTypeMap.put("ZP2015_UNIT_ARR",new EdxStringConvertor("UNIT_ARR"));
		stringTypeMap.put("ZP2015_UNIT_CONTENT",new EdxStringConvertor("UNIT_CONTENT"));
		stringTypeMap.put("ZP2015_UNIT_SOURCE",new EdxStringConvertor("UNIT_SOURCE"));
		stringTypeMap.put("ZP2015_FUTURE_UNITS",new EdxStringConvertor("FUTURE_UNITS"));
		stringTypeMap.put("ZP2015_UNIT_ACCESS",new EdxStringConvertor("UNIT_ACCESS"));
		stringTypeMap.put("ZP2015_UNIT_CURRENT_STATUS",new EdxStringConvertor("UNIT_CURRENT_STATUS"));
		stringTypeMap.put("ZP2015_ARRANGE_RULES",new EdxStringConvertor("ARRANGE_RULES"));
		stringTypeMap.put("ZP2015_ORIGINATOR",new EdxApRefConvertor("ORIGINATOR_REF",this.dataProvider));
		stringTypeMap.put("ZP2015_AP_REF",new EdxApRefConvertor("AP_REF",this.dataProvider));
		stringTypeMap.put("ZP2015_ITEM_TITLE_REF",new EdxApRefConvertor("ITEM_TITLE",this.dataProvider));
		stringTypeMap.put("ZP2015_FORMAL_TITLE",new EdxStringConvertor("FORMAL_TITLE"));
		stringTypeMap.put("ZP2015_SCALE",new EdxStringConvertor("SCALE"));
		stringTypeMap.put(ElzaTypes.ZP2015_LANGUAGE, new EdxEnumConvertor(CoreTypes.LANGUAGE, ElzaTypes.languageTypeMap));
		stringTypeMap.put(ElzaTypes.ZP2015_ORIENTATION, new EdxStringConvertor(CoreTypes.ORIENTATION));
	    stringTypeMap.put(ElzaTypes.ZP2015_ITEM_MAT, new EdxStringConvertor(CoreTypes.ITEM_MAT));
	    stringTypeMap.put(ElzaTypes.ZP2015_PART, new EdxStringConvertor(CoreTypes.PART));
		stringTypeMap.put("ZP2015_STORAGE_COND",new EdxStringConvertor("STORAGE_COND"));
		stringTypeMap.put("ZP2015_RELATED_UNITS",new EdxStringConvertor("RELATED_UNITS"));
		stringTypeMap.put("ZP2015_UNIT_DATE",new EdxUnitDateConvertor("UNIT_DATE"));
		stringTypeMap.put("ZP2015_SIZE",new EdxStringConvertor("SIZE"));
		stringTypeMap.put("ZP2015_ITEM_MAT",new EdxStringConvertor("ITEM_MAT"));
		stringTypeMap.put("ZP2015_INV_CISLO",new EdxStringConvertor("INV_CISLO"));
		stringTypeMap.put("ZP2015_OTHER_ID",new EdxStringConvertor("OTHER_ID"));
		stringTypeMap.put("ZP2015_EDITION",new EdxStringConvertor("EDITION"));
		stringTypeMap.put("ZP2015_UNIT_DATE_TEXT",new EdxStringConvertor("UNIT_DATE_TEXT"));
		stringTypeMap.put("ZP2015_EXERQUE",new EdxStringConvertor("EXERQUE"));
		stringTypeMap.put("ZP2015_PAINTING_CHAR",new EdxStringConvertor("PAINTING_CHAR"));
		stringTypeMap.put(ElzaTypes.ZP2015_CORROBORATION, new EdxStringConvertor(CoreTypes.CORROBORATION));
		stringTypeMap.put(ElzaTypes.ZP2015_IMPRINT_COUNT, new EdxStringConvertor(CoreTypes.IMPRINT_COUNT));
		stringTypeMap.put(ElzaTypes.ZP2015_IMPRINT_ORDER, new EdxStringConvertor(CoreTypes.IMPRINT_ORDER));
		stringTypeMap.put(ElzaTypes.ZP2015_LEGEND, new EdxStringConvertor(CoreTypes.LEGEND));
		stringTypeMap.put(ElzaTypes.ZP2015_MOVIE_LENGTH, new EdxTimeLenghtConvertor(CoreTypes.MOVIE_LENGTH));
		stringTypeMap.put(ElzaTypes.ZP2015_RECORD_LENGTH, new EdxTimeLenghtConvertor(CoreTypes.RECORD_LENGTH));
		stringTypeMap.put(ElzaTypes.ZP2015_WRITING, new EdxStringConvertor(CoreTypes.WRITING));
		
		stringTypeMap.put("ZP2015_EXISTING_COPY",new EdxStringConvertor("EXISTING_COPY"));
		stringTypeMap.put("ZP2015_ARRANGEMENT_INFO",new EdxStringConvertor("ARRANGEMENT_INFO"));
		stringTypeMap.put("ZP2015_ENTITY_ROLE",new EdxApRefWithRole(CoreTypes.PT_ENTITY_ROLE, CoreTypes.ROLE, CoreTypes.AP_REF,this.dataProvider, 
		                                                            ElzaTypes.roleSpecMap));
		stringTypeMap.put("ZP2015_UNIT_COUNT",new EdxNullConvertor());
		stringTypeMap.put("ZP2015_NOTE",new EdxStringConvertor(CoreTypes.NOTE));
		stringTypeMap.put("ZP2015_DESCRIPTION_DATE",new EdxStringConvertor("DESCRIPTION_DATE"));
		
		EdxItemConvertor convertor = stringTypeMap.get(item.getT());
		if(convertor!=null) {
			convertor.convert(this, item);
			return;
		}		
		
		throw new RuntimeException("Unsupported item type: " + item.getT());		
	}

	private String getName(Section sect, Level lvl) {
		String parentId = lvl.getPid();
		
		StringBuilder sb = new StringBuilder();
		
		for(DescriptionItem item :lvl.getDdOrDoOrDp()) {
			if(item.getT().equals("ZP2015_TITLE")) {
				DescriptionItemString title = (DescriptionItemString)item;
				sb.append(title.getV());
			}
		}
		
		// koren -> jmeno AS
		if(sb.length()==0) {
			if(parentId == null) {
				sb.append(sect.getFi().getN());
			}
		}
		
		return sb.toString();
	}

	@Override
	public ApuSourceBuilder getApusBuilder() {
		return apusBuilder;
	}

	@Override
	public Part getActivePart() {
		return activePart;
	}

	@Override
	public ElzaXmlReader getElzaXmlReader() {
		return elzaXmlReader;
	}

	@Override
	public void addArchEntityRef(UUID uuid) {
		apRefs.add(uuid);		
	}

	@Override
	public Apu getActiveApu() {
		return activeApu;
	}
}
