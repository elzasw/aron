package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.Validate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.NadHeader;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.PropertiesDataProvider;

public class ImportPevaFundInfo {
	
	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
    
    private ContextDataProvider dataProvider;
    
    private String institutionCode;
    
    public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);
        ImportPevaFundInfo ifi = new ImportPevaFundInfo();
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

    public ApuSourceBuilder importFundInfo(Path inputFile, String propFile) throws IOException, JAXBException {
        Validate.isTrue(propFile!=null&&propFile.length()>0);
        
        PropertiesDataProvider pdp = new PropertiesDataProvider();
        Path propPath = Paths.get(propFile);
        pdp.load(propPath);
        
        return importFundInfo(inputFile, null, pdp);
    }

    public ApuSourceBuilder importFundInfo(final Path inputFile, UUID uuid, final ContextDataProvider cdp) throws IOException, JAXBException {
    	dataProvider = cdp;
        GetNadSheetResponse gnsr = Peva2XmlReader.unmarshalGetNadSheetResponse(inputFile);
        NadSheet nadSheet = gnsr.getNadPrimarySheet();
        if (nadSheet==null) {
        	nadSheet = gnsr.getNadSubsheet();
        }                        
        importFundInfo(nadSheet);
        return apusBuilder;
    }
    
    
    private void importFundInfo(NadSheet nadSheet) {
    	
		NadPrimarySheet primarySheet = (NadPrimarySheet) nadSheet;
    	
        var institutionRef = nadSheet.getInstitution();
        institutionCode = institutionRef.getExternalId();

        NadHeader nadHeader = nadSheet.getHeader();
        var fundName = nadHeader.getName();
        var mark = nadHeader.getMark();

    	Apu apu = apusBuilder.createApu(fundName,ApuType.FUND, UUID.fromString(nadSheet.getId()));
    	Part partName = apusBuilder.addPart(apu, CoreTypes.PT_TITLE);
		partName.setValue(fundName);
		apusBuilder.addString(partName, CoreTypes.TITLE, fundName);

		var instIndo = dataProvider.getInstitutionApu(institutionCode);
		if(instIndo==null) {
            throw new RuntimeException("Missing institution: " + institutionCode);
		}

		Part partFundInfo = apusBuilder.addPart(apu, CoreTypes.PT_FUND_INFO);
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instIndo.getUuid());
		/*
		var rootLvlUuid = getRootLevelUuid(sect.getLvls());		
		if(rootLvlUuid!=null) {
			apusBuilder.addApuRef(partFundInfo, "ARCHDESC_ROOT_REF", rootLvlUuid);
		}
		*/
        if(primarySheet.getEvidenceNumber()!=null) {
            apusBuilder.addString(partFundInfo, "CISLO_NAD", primarySheet.getEvidenceNumber());
        }
        if(mark!=null) {
            apusBuilder.addString(partFundInfo, "FUND_MARK", mark);
        }        
        // datace
        if (nadHeader.getTimeRange()!=null) {
        	Peva2Utils.fillDateRange(nadHeader.getTimeRange(), partFundInfo, apusBuilder);
        }        

    }   

    public String getInstitutionCode() {
    	return institutionCode;
    }

}
