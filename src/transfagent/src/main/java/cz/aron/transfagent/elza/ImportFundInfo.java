package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.Validate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Level;
import cz.tacr.elza.schema.v2.Levels;
import cz.tacr.elza.schema.v2.Section;
import cz.tacr.elza.schema.v2.Sections;

public class ImportFundInfo {

	private ElzaXmlReader elzaXmlReader;

	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

	private ContextDataProvider dataProvider;

	private String institutionCode;
	
	public static void main(String[] args) {
		Path inputFile = Path.of(args[0]);
		ImportFundInfo ifi = new ImportFundInfo();
		try {
			ApuSourceBuilder apusrcBuilder = ifi.importFundInfo(inputFile, args[1]);
			Path ouputPath = Paths.get(args[2]);
			try(OutputStream fos = Files.newOutputStream(ouputPath)) {
				apusrcBuilder.build(fos);
			}
		} catch(Exception e) {
			System.err.println("Failed to process input file: "+inputFile);
			e.printStackTrace();
		}

	}

	private ApuSourceBuilder importFundInfo(Path inputFile, String propFile) throws IOException, JAXBException {
		Validate.isTrue(propFile!=null&&propFile.length()>0);
		
		PropertiesDataProvider pdp = new PropertiesDataProvider();
		Path propPath = Paths.get(propFile);
		pdp.load(propPath);
		
		return importFundInfo(inputFile, pdp);
	}

    public ApuSourceBuilder importFundInfo(final Path inputFile, final ContextDataProvider cdp) throws IOException, JAXBException {
        this.dataProvider = cdp;

        try (InputStream is = Files.newInputStream(inputFile)) {
            elzaXmlReader = ElzaXmlReader.read(is);
            return importFundInfo();
        }
    }

	private ApuSourceBuilder importFundInfo() {
		Sections sections = elzaXmlReader.getEdx().getFs();
		if(sections==null||sections.getS().size()==0) {
			throw new RuntimeException("Missing section data");
		}
		if(sections.getS().size()>1) {
			throw new RuntimeException("Exports with one section are supported");
		}
		Section sect = sections.getS().get(0);
		FundInfo fi = sect.getFi();
		String fundName = fi.getN();
		Apu apu = apusBuilder.createApu(fundName,ApuType.FUND);
		Part partName = apusBuilder.addPart(apu, "PT_NAME");
		partName.setValue(fundName);
		apusBuilder.addString(partName, "NAME", fundName);
		
		institutionCode = fi.getIc();
		var instApu = dataProvider.getInstitutionApu(institutionCode);
		Part partFundInfo = apusBuilder.addPart(apu, "PT_FUND_INFO");
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instApu);
		var rootLvlUuid = getRootLevelUuid(sect.getLvls());
		if(rootLvlUuid!=null) {
			apusBuilder.addApuRef(partFundInfo, "ARCHDESC_ROOT_REF", rootLvlUuid);
		}
		
		// Puvodce
		var puvodci = getPuvodci(sect.getLvls());
		for(var puvodceUuid: puvodci) {
			apusBuilder.addApuRef(partFundInfo, "ORIGINATOR_REF", puvodceUuid);
		}
		
		return apusBuilder;
	}

	private UUID getRootLevelUuid(Levels lvls) {
		if(lvls==null) {
			return null;
		}
		List<Level> lvlList = lvls.getLvl();
		if(lvlList.size()==0) {
			return null;
		}
		Level lvl = lvlList.get(0);
		return UUID.fromString(lvl.getUuid());
	}

	private List<UUID> getPuvodci(Levels lvls) {
		if(lvls==null) {
			return Collections.emptyList();
		}
		List<String> puvodciXmlId = new ArrayList<>();
		Set<String> found = new HashSet<>();
		for(Level lvl: lvls.getLvl()) {
			for(DescriptionItem item: lvl.getDdOrDoOrDp()) {
				if(item.getT().equals("ZP2015_ORIGINATOR")) {
					DescriptionItemAPRef apRef = (DescriptionItemAPRef)item;
					if(!found.contains(apRef.getApid())) {
						found.add(apRef.getApid());
						puvodciXmlId.add(apRef.getApid());
					}
				}
			}
		}
		if(found.size()==0) {
			return Collections.emptyList();
		}
		
		Map<String, AccessPoint> apMap = elzaXmlReader.getApMap();
		
		List<UUID> puvodci = new ArrayList<>(puvodciXmlId.size());
		for(String xmlId: puvodciXmlId) {
			AccessPoint ap = apMap.get(xmlId);
			if(ap==null) {
				throw new RuntimeException("Missing AP with ID: "+xmlId);
			}
			puvodci.add(UUID.fromString(ap.getApe().getUuid()));
		}
		return puvodci;
	}

	public String getInstitutionCode() {
		return institutionCode;
	}

}
