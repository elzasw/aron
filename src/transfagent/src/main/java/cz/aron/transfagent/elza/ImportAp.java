package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.JAXBException;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;

public class ImportAp {
	ElzaXmlReader elzaXmlReader;	
	
	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

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

	private ApuSourceBuilder importAp(Path inputFile, String apUuid) throws IOException, JAXBException {
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
		
		return apusBuilder;
	}

}
