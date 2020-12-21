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
import cz.aron.transfagent.transformation.CoreTypes;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Fragments;

public class ImportAp {
	ElzaXmlReader elzaXmlReader;	
	
	ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
	
	static ApTypeService apTypeService = new ApTypeService(); 
	
	Set<Integer> accesileElzaIds = new HashSet<>();

	private String apUuid; 	

	public String getApUuid() {
		return apUuid;
	}

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

	private ApuSourceBuilder importAp(final String apUuid) {
		AccessPoint ap;
		if(apUuid!=null) {
			ap = elzaXmlReader.findAccessPointByUUID(apUuid);
			if(ap==null) {
				throw new IllegalStateException("AccessPoint not found: "+apUuid);
			}
		} else {
			// find first
			ap = elzaXmlReader.getSingleAccessPoint();
			if(ap==null) {
				throw new IllegalStateException("AccessPoint not found in result");
			}
		}
		this.apUuid = ap.getApe().getUuid();

		Apu apu = apusBuilder.createApu(null, ApuType.ENTITY);
		
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
			}
		}
		if(apu==null) {
			throw new IllegalStateException("AP without name: "+apUuid);
		}
		
		
		// 
		
		return apusBuilder;
	}

	private void importBody(Apu apu, Part part, Fragment frg) {
		
		String briefDesc = ElzaXmlReader.getStringType(frg, ElzaTypes.BRIEF_DESC);
		if(StringUtils.isNotEmpty(briefDesc)) {
			apu.setDesc(briefDesc);
			
			//apusBuilder.
		}
		
		String adminPrntRefId = ElzaXmlReader.getApRef(frg, ElzaTypes.GEO_ADMIN_CLASS);
		if(StringUtils.isNotEmpty(adminPrntRefId)) {
			accesileElzaIds.add(Integer.valueOf(adminPrntRefId));
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

	public Set<Integer> getAccesileElzaIds() {
		return accesileElzaIds;
	}
}
