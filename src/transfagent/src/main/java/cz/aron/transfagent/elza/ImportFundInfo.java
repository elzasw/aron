package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.PropertiesDataProvider;
import cz.tacr.elza.schema.v2.ElzaDataExchange;
import cz.tacr.elza.schema.v2.FundInfo;
import cz.tacr.elza.schema.v2.Section;
import cz.tacr.elza.schema.v2.Sections;

public class ImportFundInfo {
	
	ElzaXmlReader elzaXmlReader = new ElzaXmlReader();	
	
	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
	
	ContextDataProvider dataProvider;	

	
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
		if(propFile!=null&&propFile.length()>0) {
			PropertiesDataProvider pdp = new PropertiesDataProvider();
			Path propPath = Paths.get(propFile);
			pdp.load(propPath);
			dataProvider = pdp;
		}
		
		try(InputStream is = Files.newInputStream(inputFile);) {
			JAXBElement<ElzaDataExchange> edxElem = ElzaXmlReader.read(is, ElzaDataExchange.class);
			ElzaDataExchange edx = edxElem.getValue();
			return importFundInfo(edx);
		}
	}

	private ApuSourceBuilder importFundInfo(ElzaDataExchange edx) {
		Sections sections = edx.getFs();
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
		
		String instCode = fi.getIc();
		String instApu = dataProvider.getInstitutionApu(instCode);
		Part partFundInfo = apusBuilder.addPart(apu, "PT_FUND_INFO");
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instApu);
		
		return apusBuilder;
	}

}
