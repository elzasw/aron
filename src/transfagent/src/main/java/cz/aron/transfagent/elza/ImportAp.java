package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.Validate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.archentities.APTypeXml;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;

public class ImportAp {
	ElzaXmlReader elzaXmlReader;	
	
	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
	
	static ApTypeService apTypeService = new ApTypeService(); 

	public ImportAp() {
		
	}
	
	public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);
		ImportAp iap = new ImportAp();
		try {
			ApuSourceBuilder apusrcBuilder = iap.importAp(inputFile, args[1]);
			Path ouputPath = Paths.get(args[2]);
			try(OutputStream fos = Files.newOutputStream(ouputPath)) {
				apusrcBuilder.build(fos);
			}
		} catch(Exception e) {
			System.err.println("Failed to process input file: "+inputFile);
			e.printStackTrace();
		}

	}

	public ApuSourceBuilder importAp(Path inputFile, String apUuid) throws IOException, JAXBException {
		try(InputStream is = Files.newInputStream(inputFile);) {
			elzaXmlReader = ElzaXmlReader.read(is);
			return importAp(apUuid);
		}
	}

	private ApuSourceBuilder importAp(String apUuid) {
		AccessPoint ap = elzaXmlReader.findAccessPointByUUID(apUuid);
		if(ap==null) {
			throw new IllegalStateException("AccessPoint not found: "+apUuid);
		}

		Apu apu = null;
		
		// extrakce dat
		
		// oznaceni
		Fragments frgs = ap.getFrgs();		
		for(Fragment frg: frgs.getFrg()) {
			if(frg.getT().equals("PT_NAME")) {			
				// add name
				if(apu==null) {
					String fullName = ElzaXmlReader.getFullName(frg);
					apu = apusBuilder.createApu(fullName, ApuType.ENTITY);
				}
				String name = ElzaXmlReader.getFullName(frg);
				apusBuilder.addName(apu, name);
			}
		}
		if(apu==null) {
			throw new IllegalStateException("AP without name: "+apUuid);
		}
		
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
		
		return apusBuilder;
	}

}
