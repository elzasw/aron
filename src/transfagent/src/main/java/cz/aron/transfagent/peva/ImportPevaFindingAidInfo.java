package cz.aron.transfagent.peva;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.FindingAid;
import cz.aron.peva2.wsdl.UniversalTimeRange;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;

public class ImportPevaFindingAidInfo {

	private ContextDataProvider dataProvider;

	private final ApuSourceBuilder apusBuilder = new ApuSourceBuilder();
	
	private final Peva2CodeLists codeLists;

	private String institutionCode;

	private List<UUID> fundUUIDs = new ArrayList<>();
	
	private UUID findingAidUUID;
	
	public ImportPevaFindingAidInfo(Peva2CodeLists codeLists) {
		this.codeLists = codeLists;
	}

	public ApuSourceBuilder importFindingAidInfo(final Path inputFile, final ContextDataProvider cdp)
			throws IOException, JAXBException {
		dataProvider = cdp;
		var gfar = Peva2XmlReader.unmarshalGetFindingAidResponse(inputFile);
		var findingAid = gfar.getFindingAid();
		importFindingAidInfo(findingAid);
		return apusBuilder;
	}

	private void importFindingAidInfo(FindingAid findingAid) {
		
		findingAidUUID = UUID.fromString(findingAid.getId());

		var findingAidName = findingAid.getName();
		Apu apu = apusBuilder.createApu(findingAidName, ApuType.FINDING_AID, UUID.fromString(findingAid.getId()));

		Part partTitle = apusBuilder.addPart(apu, CoreTypes.PT_TITLE);
		partTitle.setValue(findingAidName);
		apusBuilder.addString(partTitle, CoreTypes.TITLE, findingAidName);

		institutionCode = findingAid.getInstitution().getExternalId();
		var instInfo = dataProvider.getInstitutionApu(institutionCode);
		Validate.notNull(instInfo, "Missing institution, code: %s", institutionCode);

		// TODO vice NadSheet
		
		for(var fundCode:findingAid.getNadSheets().getNadSheet()) {
			var fundUUID = dataProvider.getFundApuByUUID(institutionCode, UUID.fromString(fundCode));			
			Validate.notNull(fundUUID, "Missing fund, code: %s, institution: %s", fundCode, institutionCode);
			fundUUIDs.add(fundUUID);
		}
		Validate.isTrue(!fundUUIDs.isEmpty(), "Fainding aid code: %, institution:%s not related to any fund", findingAid.getEvidenceNumber(),
				institutionCode);

		// add info part
		Part partInfo = apusBuilder.addPart(apu, CoreTypes.PT_FINDINGAID_INFO);
		apusBuilder.addString(partInfo, CoreTypes.FINDINGAID_ID, findingAid.getEvidenceNumber());
		
		/**
		var releaseDatePlace = ead3XmlReader.getReleaseDatePlace();
		if (StringUtils.isNotEmpty(releaseDatePlace)) {
			apusBuilder.addString(partInfo, CoreTypes.FINDINGAID_RELEASE_DATE_PLACE, releaseDatePlace);
		}*/
						
		var findingAidType = codeLists.getFindingAidTypes().get(findingAid.getType());
		if (StringUtils.isNotEmpty(findingAidType)) {
			apusBuilder.addEnum(partInfo, CoreTypes.FINDINGAID_TYPE, findingAidType);
		}
		var dateRange = findingAid.getTimeRange();
		if (dateRange != null) {
			fillFindingAidDateRange(dateRange, partInfo);
		}
		/**
		var unitsAmount = ead3XmlReader.getLocalionControlByType("UNITS_AMOUNT");
		if (StringUtils.isNotEmpty(unitsAmount)) {
			apusBuilder.addString(partInfo, CoreTypes.FINDINGAID_UNITS_AMOUNT, unitsAmount);
		}*/

		// add references part		
		var partRef = apusBuilder.addPart(apu, CoreTypes.PT_ARCH_DESC_FUND);
		for(var fundUUID:fundUUIDs) {
			apusBuilder.addApuRef(partRef, "FUND_REF", fundUUID);
		}
		apusBuilder.addApuRef(partRef, "FUND_INST_REF", instInfo.getUuid());

	}

	private void fillFindingAidDateRange(UniversalTimeRange timeRange, Part partInfo) {
		apusBuilder.addString(partInfo, CoreTypes.FINDINGAID_DATE_RANGE,
				"" + timeRange.getTimeRangeFrom() + " - " + timeRange.getTimeRangeTo());		
		Peva2Utils.fillDateRange(timeRange, partInfo, apusBuilder);
	}

	public String getInstitutionCode() {
		return institutionCode;
	}
	
	public List<UUID> getFundUUIDs() {
		return fundUUIDs;
	}

	public UUID getFindingAidUUID() {
		return findingAidUUID;
	}

}
