package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.convertor.EdxApRefConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemConvertor;
import cz.aron.transfagent.elza.convertor.EdxItemCovertContext;
import cz.aron.transfagent.elza.convertor.EdxStringConvertor;
import cz.aron.transfagent.elza.convertor.EdxUnitDateConvertor;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Levels;
import cz.tacr.elza.schema.v2.Section;
import cz.tacr.elza.schema.v2.Sections;

public class ImportArchDesc implements EdxItemCovertContext {
	ElzaXmlReader elzaXmlReader;	
	
	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
	
	ContextDataProvider dataProvider;
	
	Map<Apu, Apu> apuParentMap = new HashMap<>();

	private Part activePart;

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

	private ApuSourceBuilder importArchDesc(Path inputFile, String propFile) throws IOException, JAXBException {
		if(propFile!=null&&propFile.length()>0) {
			PropertiesDataProvider pdp = new PropertiesDataProvider();
			Path propPath = Paths.get(propFile);
			pdp.load(propPath);
			dataProvider = pdp;
		}
		
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
			
			// add items	
			for(DescriptionItem item: lvl.getDdOrDoOrDp()) {
				addItem(apu, item);
			}
		}		
		return apusBuilder;
	}

	private void addItem(Apu apu, DescriptionItem item) {
		final String ignoredTypes[] = {"ZP2015_ARRANGEMENT_TYPE", "ZP2015_LEVEL_TYPE",
				"ZP2015_SERIAL_NUMBER", "ZP2015_NAD", "ZP2015_ZNACKA_FONDU",
				"ZP2015_LEVEL_TYPE", "ZP2015_ARRANGER",
				"ZP2015_UNIT_DATE_BULK","ZP2015_FOLDER_TYPE",
				"ZP2015_STORAGE_ID", "ZP2015_ITEM_ORDER",
				"ZP2015_UNIT_COUNT_ITEM",
				"ZP2015_INTERNAL_NOTE", "ZP2015_POSITION",
		// TODO: k zapracovani
				"ZP2015_LANGUAGE",
				"ZP2015_DATE_OTHER",
				"ZP2015_UNIT_TYPE",
				"ZP2015_ENTITY_ROLE"
		};
		
		// check ignored items
		for(String ignoredType: ignoredTypes) {
			if(ignoredType.equals(item.getT())) {
				return;
			}
		}
		
		activePart = null;
		if(apu.getPrts()!=null) {
			for (Part p : apu.getPrts().getPart()) {
				if (p.getType().equals("PT_ARCH_DESC")) {
					activePart = p;
					break;
				}
			}
		}
		if(activePart==null) {
			activePart = apusBuilder.addPart(apu, "PT_ARCH_DESC");
		}
		
		Map<String, EdxItemConvertor> stringTypeMap = new HashMap<>();
		stringTypeMap.put("ZP2015_TITLE",new EdxStringConvertor("ABSTRACT"));
		stringTypeMap.put("ZP2015_UNIT_ID",new EdxStringConvertor("UNIT_ID"));
		stringTypeMap.put("ZP2015_UNIT_HIST",new EdxStringConvertor("HISTORY"));
		stringTypeMap.put("ZP2015_UNIT_ARR",new EdxStringConvertor("UNIT_ARR"));
		stringTypeMap.put("ZP2015_UNIT_CONTENT",new EdxStringConvertor("UNIT_CONTENT"));
		stringTypeMap.put("ZP2015_UNIT_SOURCE",new EdxStringConvertor("UNIT_SOURCE"));
		stringTypeMap.put("ZP2015_FUTURE_UNITS",new EdxStringConvertor("FUTURE_UNITS"));
		stringTypeMap.put("ZP2015_UNIT_ACCESS",new EdxStringConvertor("UNIT_ACCESS"));
		stringTypeMap.put("ZP2015_UNIT_CURRENT_STATUS",new EdxStringConvertor("UNIT_CURRENT_STATUS"));
		stringTypeMap.put("ZP2015_ARRANGE_RULES",new EdxStringConvertor("ARRANGE_RULES"));
		stringTypeMap.put("ZP2015_ORIGINATOR",new EdxApRefConvertor("ORIGINATOR_REF"));
		stringTypeMap.put("ZP2015_AP_REF",new EdxApRefConvertor("APU_REF"));
		stringTypeMap.put("ZP2015_ITEM_TITLE_REF",new EdxApRefConvertor("APU_REF"));
		stringTypeMap.put("ZP2015_FORMAL_TITLE",new EdxStringConvertor("FORMAL_TITLE"));
		stringTypeMap.put("ZP2015_SCALE",new EdxStringConvertor("SCALE"));
		stringTypeMap.put("ZP2015_STORAGE_COND",new EdxStringConvertor("STORAGE_COND"));
		stringTypeMap.put("ZP2015_RELATED_UNITS",new EdxStringConvertor("RELATED_UNITS"));
		stringTypeMap.put("ZP2015_UNIT_DATE",new EdxUnitDateConvertor("UNIT_DATE"));
		
		EdxItemConvertor convertor = stringTypeMap.get(item.getT());
		if(convertor!=null) {
			convertor.convert(this, item);
			activePart = null;
			return;
		}
		
		activePart = null;
		
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
}
