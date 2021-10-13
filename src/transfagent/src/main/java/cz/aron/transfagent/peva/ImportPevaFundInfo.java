package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.StringJoiner;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.util.StringUtil;
import org.springframework.util.CollectionUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.EvidenceUnitProcedure;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.NadHeader;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.peva2.wsdl.NadSubsheet;
import cz.aron.transfagent.config.ConfigPeva2FundProperties;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;
import cz.aron.transfagent.transformation.InstitutionInfo;
import cz.aron.transfagent.transformation.PropertiesDataProvider;

public class ImportPevaFundInfo {
		
	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
    
    private ContextDataProvider dataProvider;
    
    private Peva2CodeListProvider codeListProvider;
    
    private ConfigPeva2FundProperties fundProperties;
    
    private String institutionCode;
    
    public static void main(String[] args) {
        Path inputFile = Path.of(args[0]);
        ImportPevaFundInfo ifi = new ImportPevaFundInfo(new ConfigPeva2FundProperties());
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
    
    public ImportPevaFundInfo(ConfigPeva2FundProperties fundProperties) {
    	if (fundProperties!=null) {
    		this.fundProperties = fundProperties;
    	} else {
    		this.fundProperties = new ConfigPeva2FundProperties();
    	}
    }

    public ApuSourceBuilder importFundInfo(Path inputFile, String propFile) throws IOException, JAXBException {
        Validate.isTrue(propFile!=null&&propFile.length()>0);
        
        PropertiesDataProvider pdp = new PropertiesDataProvider();
        Path propPath = Paths.get(propFile);
        pdp.load(propPath);
        
        return importFundInfo(inputFile, null, pdp, null, null, null);
    }

	public ApuSourceBuilder importFundInfo(final Path inputFile, UUID uuid, final ContextDataProvider cdp,
			final FundProvider fundProvider, final Peva2CodeListProvider codeListProvider, Peva2CachedEntityDownloader entityDownloader)
			throws IOException, JAXBException {
		dataProvider = cdp;
		this.codeListProvider = codeListProvider;
		GetNadSheetResponse gnsr = Peva2XmlReader.unmarshalGetNadSheetResponse(inputFile);
		if (gnsr.getNadPrimarySheet() != null) {
			importPrimarySheet(gnsr.getNadPrimarySheet());
		} else {
			var parentUUID = gnsr.getNadSubsheet().getParent();
			NadPrimarySheet primarySheet = fundProvider.getFundByUUID(UUID.fromString(parentUUID));
			importNadSubsheet(gnsr.getNadSubsheet(), primarySheet);
		}
		return apusBuilder;
	}
    
    private void importNadSubsheet(NadSubsheet subsheet, NadPrimarySheet primarySheet)  throws IOException, JAXBException {

    	var instInfo = getInstitutionInfo(subsheet);
        var nadHeader = subsheet.getHeader();
        var fundName = nadHeader.getName();        
        var fullFundName = createFundName(instInfo, primarySheet);

    	Apu apu = apusBuilder.createApu(fundName,ApuType.FUND, UUID.fromString(subsheet.getId()));
    	Part partName = ApuSourceBuilder.addPart(apu, CoreTypes.PT_TITLE);
		partName.setValue(fundName);
		ApuSourceBuilder.addString(partName, CoreTypes.TITLE, fullFundName);
		
		Part partFundInfo = ApuSourceBuilder.addPart(apu, CoreTypes.PT_FUND_INFO);
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instInfo.getUuid());

        processEvidenceNumber(primarySheet,subsheet,partFundInfo);
        processNadHeaderCommon(nadHeader, partFundInfo);
        processNadSheetSubNadSheetCommon(subsheet, partFundInfo);
    }
    
    
    private void importPrimarySheet(NadPrimarySheet primarySheet) {
    	
    	var instInfo = getInstitutionInfo(primarySheet);   
        var nadHeader = primarySheet.getHeader();
        var fundName = nadHeader.getName();
        var fullFundName = createFundName(instInfo, primarySheet);

    	Apu apu = apusBuilder.createApu(fundName,ApuType.FUND, UUID.fromString(primarySheet.getId()));
    	Part partName = apusBuilder.addPart(apu, CoreTypes.PT_TITLE);
		partName.setValue(fundName);
		ApuSourceBuilder.addString(partName, CoreTypes.TITLE, fullFundName);

		Part partFundInfo = apusBuilder.addPart(apu, CoreTypes.PT_FUND_INFO);
		apusBuilder.addApuRef(partFundInfo, "INST_REF", instInfo.getUuid());
	
		processEvidenceNumber(primarySheet, null, partFundInfo);	
        processNadHeaderCommon(nadHeader, partFundInfo);
		processNadSheetSubNadSheetCommon(primarySheet, partFundInfo);        
    }
    
	private InstitutionInfo getInstitutionInfo(NadSheet nadSheet) {
		var instInfo = dataProvider.getInstitutionApu(nadSheet.getInstitution().getExternalId());
		if (instInfo == null) {
			throw new RuntimeException("Missing institution: " + institutionCode);
		}
		institutionCode = nadSheet.getInstitution().getExternalId();
		return instInfo;
	}
    
	private String createFundName(InstitutionInfo institutionInfo, NadSheet nadSheet) {
		if (fundProperties.isComposedFundName()) {
			return institutionInfo.getName() + ": " + nadSheet.getHeader().getName() + " "
					+ (nadSheet.getHeader().getTimeRange()!=null?Peva2Utils.getAsString(nadSheet.getHeader().getTimeRange()):"");
		} else {
			return nadSheet.getHeader().getName();
		}
	}

	private void processEvidenceNumber(NadPrimarySheet primarySheet, NadSubsheet subsheet, Part partFundInfo) {
		if (StringUtils.isNotBlank(primarySheet.getEvidenceNumber())) {
			if (subsheet!=null&&subsheet.getNumber()!=null) {
				ApuSourceBuilder.addString(partFundInfo, "CISLO_NAD", primarySheet.getEvidenceNumber()+"/"+subsheet.getNumber());
			} else {
				ApuSourceBuilder.addString(partFundInfo, "CISLO_NAD", primarySheet.getEvidenceNumber());			
			}
		}		
	}
	
    private void processNadHeaderCommon(NadHeader nadHeader, Part partFundInfo) {
    	var mark = nadHeader.getMark();
        if(mark!=null) {
        	ApuSourceBuilder.addString(partFundInfo, "FUND_MARK", mark);
        }        
        // datace
        if (nadHeader.getTimeRange()!=null) {
        	Peva2Utils.fillDateRange(nadHeader.getTimeRange(), partFundInfo, apusBuilder);
        }
        // ke kdy jsou udaje platne
		if (fundProperties.isToDate()&&nadHeader.getToDate()!=null) {
			ApuSourceBuilder.addString(partFundInfo, "FUND_UPTODATE", nadHeader.getToDate().toString());
		}
    }
    
	private void processNadSheetSubNadSheetCommon(NadSheet nadSheet, Part partFundInfo) {
		generateDescription(nadSheet, partFundInfo);
		processAdditionalInfo(nadSheet, partFundInfo);
		processFindingAids(nadSheet, partFundInfo);
		processOriginators(nadSheet, partFundInfo);
		processLanguages(nadSheet, partFundInfo);
		processEvidenceUnits(nadSheet, partFundInfo);
		processPlacesOfOrigin(nadSheet, partFundInfo);
		processPreservationStatus(nadSheet, partFundInfo);
	}
	
	private void generateDescription(NadSheet nadSheet, Part partFundInfo) {
		
		var sj = new StringJoiner("\r\n");
		boolean add = false;
		
		var additionalInfo = nadSheet.getAdditionalInfo();		
		// prida popis puvodce do main apu
		if (fundProperties.isOriginatorAsDescription()&&StringUtils.isNotBlank(additionalInfo.getOriginator())) {
			sj.add(correctString(additionalInfo.getOriginator()));
			add = true;
		}
		if (fundProperties.isNoteAsDescription()&&StringUtil.isNotBlank(additionalInfo.getNote())) {
			sj.add(correctString(additionalInfo.getNote()));
			add = true;
		}
		if (fundProperties.isParseInternalChangesAsDescription()&& StringUtils.isNotBlank(additionalInfo.getInternalChanges())) {
			additionalInfo.getInternalChanges().lines().findFirst().ifPresent(l -> {
				sj.add(l);
			});		
			add = true;
		}
		if (add) {
			apusBuilder.getMainApu().setDesc(sj.toString());
		}
	}
    
	private void processAdditionalInfo(NadSheet nadSheet, Part partFundInfo) {
		var additionalInfo = nadSheet.getAdditionalInfo();
		if (additionalInfo != null) {
			if (StringUtils.isNotBlank(additionalInfo.getNote())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_NOTE", correctString(additionalInfo.getNote()));
			}
			if (StringUtils.isNotBlank(additionalInfo.getOriginator())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_ORIG_NOTE",
						correctString(additionalInfo.getOriginator()));
			}
			if (StringUtils.isNotBlank(additionalInfo.getThematicDescription())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_TOPIC",
						correctString(additionalInfo.getThematicDescription()));
			}
			if (StringUtils.isNotBlank(additionalInfo.getEdition())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_EDITIONS", correctString(additionalInfo.getEdition()));
			} else if (fundProperties.isParseInternalChanges()
					&& StringUtils.isNotBlank(additionalInfo.getInternalChanges())) {
				additionalInfo.getInternalChanges().lines().findFirst().ifPresent(l -> {
					ApuSourceBuilder.addString(partFundInfo, "FUND_EDITIONS", correctString(l));
				});
			}
			if (fundProperties.isLiterature() && StringUtils.isNotBlank(additionalInfo.getLiterature())) {
				ApuSourceBuilder.addString(partFundInfo, "FUND_LITERATURE",
						correctString(additionalInfo.getLiterature()));
			}
		}
	}

    private void processFindingAids(NadSheet nadSheet, Part partFundInfo) {
    	var findingAids = nadSheet.getFindingAids();
		if (findingAids!=null&&findingAids.getFindingAid()!=null) {
			findingAids.getFindingAid().forEach(fa->{
				apusBuilder.addApuRef(partFundInfo, "FINDINGAID_REF", UUID.fromString(fa));				
			});			
		}	
    }
    
    private void processOriginators(NadSheet nadSheet, Part partFundInfo) {
		var originators = nadSheet.getOriginators();
		if (originators!=null&&originators.getOriginator()!=null) {
			originators.getOriginator().forEach(o->{
				apusBuilder.addApuRef(partFundInfo, CoreTypes.ORIGINATOR_REF, UUID.fromString(o));		
			});
		}
    }
    
	private void processLanguages(NadSheet nadSheet, Part partFundInfo) {
		if (fundProperties.isLanguages()) {
			var languages = nadSheet.getLanguageRecords();
			for (var language : languages.getLanguageRecord()) {
				var p2Lang = codeListProvider.getCodeLists().getLanguage(language.getLanguage());
				if (p2Lang != null) {
					ApuSourceBuilder.addEnum(partFundInfo, CoreTypes.LANGUAGE, p2Lang.getName());
				}
			}
		}
	}
	
	private void processEvidenceUnits(NadSheet nadSheet, Part partFundInfo) {
		if (fundProperties.isEvidenceUnits()) {
			var evidenceUnits = nadSheet.getEvidenceUnits();
			if (evidenceUnits!=null) {
				for(var evidenceUnit:evidenceUnits.getEvidenceUnit()) {					
					StringJoiner sj = new StringJoiner(", ");					
					var type = evidenceUnit.getEvidenceUnitType();
					var typeName = codeListProvider.getCodeLists().getEvidenceUnitType(type.getValue());
					if (typeName!=null) {
						sj.add(typeName.getName());
					}					
					processEvidenceUnitProcedure("Velikost v digitální podobě", evidenceUnit.getDigitalProcedure(), sj);
					processEvidenceUnitProcedure("Počet", evidenceUnit.getCountProcedure(), sj);
					processEvidenceUnitProcedure("Metráž", evidenceUnit.getLengthProcedure(), sj);
					ApuSourceBuilder.addString(partFundInfo, "EVIDENCE_UNIT", sj.toString());
				}
			}
		}
	}

	private void processEvidenceUnitProcedure(String name, EvidenceUnitProcedure evidenceUnitProcedure, StringJoiner sj) {
		boolean add = false;
		StringJoiner sjInt = new StringJoiner(", ", name, "");
		if (evidenceUnitProcedure == null) {
			return;
		}
		if (evidenceUnitProcedure.getDamaged().getAmount().compareTo(BigDecimal.ZERO) > 0) {
			sjInt.add("Poškozeno: " + evidenceUnitProcedure.getDamaged().getAmount());
			add = true;
		}
		if (evidenceUnitProcedure.getInventory().getAmount().compareTo(BigDecimal.ZERO) > 0) {
			sjInt.add("Inventarizováno: " + evidenceUnitProcedure.getInventory().getAmount());
			add = true;
		}
		if (evidenceUnitProcedure.getNotProcessed().getAmount().compareTo(BigDecimal.ZERO) > 0) {
			sjInt.add("Nezpracováno: " + evidenceUnitProcedure.getNotProcessed().getAmount());
			add = true;
		}
		if (evidenceUnitProcedure.getProcessed().getAmount().compareTo(BigDecimal.ZERO) > 0) {
			sjInt.add("Zpracováno: " + evidenceUnitProcedure.getProcessed().getAmount());
			add = true;
		}		
		if (add) {
			sj.add(sjInt.toString());
		}
	}
	
	private void processPlacesOfOrigin(NadSheet nadSheet, Part partFundInfo) {
		if (!fundProperties.isPlacesOfOrigin()) {
			return;
		}
		//var places = new TreeSet<String>();
		var placesOfOrigin = nadSheet.getPlacesOfOrigin();
		for(var placeOfOrigin:placesOfOrigin.getPlaceOfOrigin()) {
			var placeId = placeOfOrigin.getPlace();
			//var geo = entityDownloader.getGeoObject(placeId);
			//var name = geo.getPreferredName().getPrimaryPart();
			//places.add(name);								
			apusBuilder.addApuRef(partFundInfo, "ORIG_PLACES_REF", UUID.fromString(placeId));			
		}
		
	}
	
	private void processPreservationStatus(NadSheet nadSheet, Part partFundInfo) {
		if (!fundProperties.isPreservationStatus()) {
			return;
		}
		if (nadSheet.getPreservationStatus() != null) {
			StringJoiner sj = new StringJoiner(", ");
			boolean add = false;
			var preservationStatus = nadSheet.getPreservationStatus();
			if (preservationStatus.getIntegrity() != null) {
				var integrity = codeListProvider.getCodeLists().getIntegrityName(preservationStatus.getIntegrity());
				sj.add(integrity);
				add = true;
			}
			if (preservationStatus.getPhysicalState() != null) {
				var physicalState = codeListProvider.getCodeLists()
						.getPhysicalStateName(preservationStatus.getPhysicalState());
				sj.add(physicalState);
				add = true;
			}
			if (preservationStatus.getDamages() != null
					&& !CollectionUtils.isEmpty(preservationStatus.getDamages().getDamage())) {
				StringJoiner sjDmg = new StringJoiner(", ", "Poškození:", "");
				for (var damage : preservationStatus.getDamages().getDamage()) {
					switch (damage.getType()) {
					case FRAGILE:
						sjDmg.add("Křehké");
						break;
					case TORN:
						sjDmg.add("Roztržené");
						break;
					case MOLD:
						sjDmg.add("Plíseň");
						break;
					case INSECT:
						sjDmg.add("Hmyz");
						break;
					case RODENT:
						sjDmg.add("Hlodavci");
						break;
					case FIRE:
						sjDmg.add("Oheň");
						break;
					case HEAT:
						sjDmg.add("Žár");
						break;
					case WATER:
						sjDmg.add("Voda");
						break;
					case CORROSION:
						sjDmg.add("Koroze");
						break;
					case EMP:
						sjDmg.add("EMP");
						break;
					case DUST:
						sjDmg.add("Prach");
						break;
					case LIGHT:
						sjDmg.add("Světlo");
						break;
					case OTHER:
						sjDmg.add("Jiné");
						break;
					}
					if (StringUtils.isNotBlank(damage.getNote())) {
						sjDmg.add(damage.getNote());
					}
					add = true;
				}
				sj.add(sjDmg.toString());
			}
			if (add) {
				apusBuilder.addString(partFundInfo, "FUND_PRESERVATION", sj.toString());
			}
		}
	}
	
	
	private String correctString(String original) {
		if (fundProperties.isCorrectLineSeparators()) {
			return Peva2Utils.correctLineSeparators(original);
		} else {
			return original;
		}				
	}
	
	
	
    public String getInstitutionCode() {
    	return institutionCode;
    }
    
	public interface FundProvider {		
		NadPrimarySheet getFundByUUID(UUID uuid) throws IOException, JAXBException;		
	}

}
