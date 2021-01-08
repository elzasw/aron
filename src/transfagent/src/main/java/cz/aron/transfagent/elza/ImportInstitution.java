package cz.aron.transfagent.elza;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;
import cz.tacr.elza.schema.v2.Institution;

public class ImportInstitution {
	
	private ElzaXmlReader elzaXmlReader;	
	
	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

	private UUID apRefUuid;
	
	public ImportInstitution() {
		
	}
	
	public UUID getApRefUuid() {
		return apRefUuid;
	}

	public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);
		ImportInstitution ii = new ImportInstitution();
		try {
			ApuSourceBuilder apusrcBuilder = ii.importInstitution(inputFile, args[1], null);
			Path ouputPath = Paths.get(args[2]);
			try(OutputStream fos = Files.newOutputStream(ouputPath)) {
				apusrcBuilder.build(fos);
			}
		} catch(Exception e) {
			System.err.println("Failed to process input file: "+inputFile);
			e.printStackTrace();
		}

	}

	public ApuSourceBuilder importInstitution(Path inputFile, String instCode, UUID uuid) throws IOException, JAXBException {
		try(InputStream is = Files.newInputStream(inputFile);) {
			elzaXmlReader = ElzaXmlReader.read(is);
			return importInstitution(instCode, uuid);
		}
	}

	/**
	 * 
	 * @param instCode
	 * @param instUuid Optional UUID of institution. If already assigned.
	 * @return
	 */
	
	private ApuSourceBuilder importInstitution(String instCode, UUID instUuid) {
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
		
		// vyhledani prvniho vhodneho oznaceni
		
		// oznaceni
		
		Fragment prefName = null;
		Fragments frgs = ap.getFrgs();		
		for(Fragment frg: frgs.getFrg()) {
			if(frg.getT().equals("PT_NAME")) {
				if(prefName==null) {
					prefName = frg;
				}
				// zjisteni zda je zkratka				
				String shortCode = ElzaXmlReader.getSingleEnum(frg, ElzaTypes.NM_TYPE);
				if(shortCode!=null) {
					if(shortCode.equals(ElzaTypes.NT_ACRONYM)) {
						prefName = frg;
						break;
					}
				}
			}
		}
		if(prefName==null) {
			throw new IllegalStateException("Institution without name: "+instCode);
		}
		// add name
		String fullName = ElzaXmlReader.getFullName(prefName);
		
		apu = apusBuilder.createApu(fullName, ApuType.INSTITUTION, instUuid);
		
		// kod archivu
		Part infoPart = apusBuilder.addPart(apu, "PT_INST_INFO");
		apusBuilder.addString(infoPart, "INST_CODE", instCode);
		
		// odkaz na entitu
		apRefUuid = UUID.fromString(ap.getApe().getUuid());
		if(apRefUuid!=null) {
			apusBuilder.addApuRef(infoPart, "AP_REF", apRefUuid);
		}
		
		
		return apusBuilder;
	}
}
