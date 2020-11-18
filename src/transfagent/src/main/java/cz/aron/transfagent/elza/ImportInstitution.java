package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.ElzaDataExchange;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;
import cz.tacr.elza.schema.v2.Institution;
import cz.tacr.elza.schema.v2.Institutions;

public class ImportInstitution {
	
	ElzaXmlReader elzaXmlReader;	
	
	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
	
	public ImportInstitution() {
		
	}

	public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);
		ImportInstitution ii = new ImportInstitution();
		try {
			ApuSourceBuilder apusrcBuilder = ii.importInstitution(inputFile, args[1]);
			Path ouputPath = Paths.get(args[2]);
			try(OutputStream fos = Files.newOutputStream(ouputPath)) {
				apusrcBuilder.build(fos);
			}
		} catch(Exception e) {
			System.err.println("Failed to process input file: "+inputFile);
			e.printStackTrace();
		}

	}

	private ApuSourceBuilder importInstitution(Path inputFile, String instCode) throws IOException, JAXBException {
		try(InputStream is = Files.newInputStream(inputFile);) {
			elzaXmlReader = ElzaXmlReader.read(is);
			return importInstitution(instCode);
		}
	}

	private ApuSourceBuilder importInstitution(String instCode) {
		Institution inst = elzaXmlReader.findInstitution(instCode);
		if(inst==null) {
			throw new IllegalStateException("Institution not found: "+instCode);
		}
		String paid = inst.getPaid();
		AccessPoint ap = elzaXmlReader.findAccessPointByUUID(paid);
		if(ap==null) {
			throw new IllegalStateException("AccessPoint for institution not found: "+instCode);
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
					apu = apusBuilder.createApu(fullName, ApuType.INSTITUTION);
				}
				String name = ElzaXmlReader.getFullName(frg);
				apusBuilder.addName(apu, name);
			}
		}
		if(apu==null) {
			throw new IllegalStateException("Institution without name: "+instCode);
		}
		
		// kod archivu
		Part infoPart = apusBuilder.addPart(apu, "PT_INST_INFO");
		apusBuilder.addString(infoPart, "INST_CODE", instCode);
		
		// odkaz na entitu
		String apRefUuid = ap.getApe().getUuid();
		if(apRefUuid!=null) {
			apusBuilder.addApuRef(infoPart, "AP_REF", apRefUuid);
		}
		
		
		return apusBuilder;
	}
}
