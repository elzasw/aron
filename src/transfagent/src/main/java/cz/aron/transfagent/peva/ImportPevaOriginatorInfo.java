package cz.aron.transfagent.peva;

import java.io.IOException;
import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.Part;
import cz.aron.peva2.wsdl.Dynasty;
import cz.aron.peva2.wsdl.DynastyName;
import cz.aron.peva2.wsdl.Event;
import cz.aron.peva2.wsdl.EventName;
import cz.aron.peva2.wsdl.Originator;
import cz.aron.peva2.wsdl.PartyGroup;
import cz.aron.peva2.wsdl.PartyGroupName;
import cz.aron.peva2.wsdl.Person;
import cz.aron.peva2.wsdl.PersonName;
import cz.aron.transfagent.elza.ApTypeService;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.aron.transfagent.transformation.CoreTypes;

public class ImportPevaOriginatorInfo {

	private final ApTypeService apTypeService;

	private final Peva2CodeListProvider codeLists;

	private ApuSourceBuilder apusBuilder = new ApuSourceBuilder();

	private String uuid;

	public ImportPevaOriginatorInfo(ApTypeService apTypeService, Peva2CodeListProvider codeLists) {
		this.apTypeService = apTypeService;
		this.codeLists = codeLists;
	}

	public ApuSourceBuilder importOriginator(Path inputFile, ContextDataProvider cdp)
			throws IOException, JAXBException {

		var getOriginatorResp = Peva2XmlReader.unmarshalGetOriginatorResponse(inputFile);
		if (getOriginatorResp.getDynasty() != null) {
			importDynasty(getOriginatorResp.getDynasty());
		} else if (getOriginatorResp.getEvent() != null) {
			importEvent(getOriginatorResp.getEvent());
		} else if (getOriginatorResp.getPartyGroup() != null) {
			importPartyGroup(getOriginatorResp.getPartyGroup());
		} else if (getOriginatorResp.getPerson() != null) {
			importPerson(getOriginatorResp.getPerson());
		} else {
			throw new IllegalStateException("Unsupported type of originator, path= {}" + inputFile);
		}
		return apusBuilder;
	}

	private void importDynasty(Dynasty dynasty) {
		var fullName = getFullName(dynasty.getPreferredName());
		var apu = apusBuilder.createApu(fullName, ApuType.ENTITY, UUID.fromString(dynasty.getId()));
		apu.setDesc(dynasty.getDescription());
		var part = ApuSourceBuilder.addPart(apu, CoreTypes.PT_AE_INFO);
		processClassSubclass(part, dynasty.getOClass().toString(), dynasty.getSubClass());
		processOriginator(part, dynasty);
	}

	private void importEvent(Event event) {
		var fullName = getFullName(event.getPreferredName());
		var apu = apusBuilder.createApu(fullName, ApuType.ENTITY, UUID.fromString(event.getId()));
		apu.setDesc(event.getDescription());
		var part = ApuSourceBuilder.addPart(apu, CoreTypes.PT_AE_INFO);
		processClassSubclass(part, event.getOClass().toString(), event.getSubClass());
		processOriginator(part, event);
	}

	private void importPartyGroup(PartyGroup partyGroup) {
		var fullName = getFullName(partyGroup.getPreferredName());
		var apu = apusBuilder.createApu(fullName, ApuType.ENTITY, UUID.fromString(partyGroup.getId()));
		apu.setDesc(partyGroup.getDescription());
		var part = ApuSourceBuilder.addPart(apu, CoreTypes.PT_AE_INFO);
		processClassSubclass(part, partyGroup.getOClass().toString(), partyGroup.getSubClass());
		processOriginator(part, partyGroup);
	}

	private void importPerson(Person person) {
		var fullName = getFullName(person.getPreferredName());
		var apu = apusBuilder.createApu(fullName, ApuType.ENTITY, UUID.fromString(person.getId()));
		apu.setDesc(person.getDescription());
		var part = ApuSourceBuilder.addPart(apu, CoreTypes.PT_AE_INFO);
		processClassSubclass(part, person.getOClass().toString(), person.getSubClass());
		processOriginator(part, person);
	}

	private void processClassSubclass(Part part, String oClass, String subclassId) {
		var tmp = codeLists.getCodeLists().getOriginatorSubClass(subclassId);
		if (tmp == null) {
			throw new IllegalStateException("Unknown subclass " + subclassId);
		}
		ApuSourceBuilder.addEnum(part, CoreTypes.AE_SUBCLASS, tmp.getName(), true);
		String parentEcName = apTypeService.getParentName(tmp.getCamCode());
		if (parentEcName != null) {
			ApuSourceBuilder.addEnum(part, CoreTypes.AE_CLASS, parentEcName, true);
		}
		ApuSourceBuilder.addEnum(part, "AE_ORIGINATOR", "ANO", false);		
	}

	private void processOriginator(Part part, Originator originator) {
		apusBuilder.setUuid(UUID.fromString(originator.getId()));
		if (StringUtils.isNotBlank(originator.getNote())) {
			ApuSourceBuilder.addString(part, CoreTypes.NOTE, originator.getNote());
		}
		if (originator.getDating() != null) {
			var dating = originator.getDating();
			if (dating.getOriginDate() != null) {
				var datingMethod = codeLists.getCodeLists().getDatingMethod(dating.getOriginMethod());
				var dateRange = parseOriginatorDating(dating.getOriginDate(), Peva2Utils.transformCamCode(datingMethod.getCamCode()));
				ApuSourceBuilder.addDateRange(part, dateRange);
			}
			if (dating.getEndDate() != null) {
				var datingMethod = codeLists.getCodeLists().getDatingMethod(dating.getEndMethod());
				var dateRange = parseOriginatorDating(dating.getEndDate(), Peva2Utils.transformCamCode(datingMethod.getCamCode()));
				ApuSourceBuilder.addDateRange(part, dateRange);
			}
		}
	}

	private String getFullName(DynastyName dynastyName) {
		StringBuilder sb = new StringBuilder();
		sb.append(dynastyName.getPrimaryPart());
		if (StringUtils.isNotBlank(dynastyName.getSecondaryPart())) {
			sb.append(", ").append(dynastyName.getSecondaryPart());
		}
		if (StringUtils.isNotBlank(dynastyName.getGeneralSupplement())) {
			sb.append(" (").append(dynastyName.getGeneralSupplement()).append(")");
		}
		return sb.toString();
	}

	private String getFullName(EventName eventName) {
		StringBuilder sb = new StringBuilder();
		sb.append(eventName.getPrimaryPart());

		var supplements = new StringJoiner(" : ", "(", ")");
		if (StringUtils.isNotBlank(eventName.getGeneralSupplement())) {
			supplements.add(eventName.getGeneralSupplement());
		}
		if (StringUtils.isNotBlank(eventName.getChronologicalSupplement())) {
			supplements.add(eventName.getChronologicalSupplement());
		}
		if (StringUtils.isNotBlank(eventName.getGeoSupplement())) {
			supplements.add(eventName.getGeoSupplement());
		}
		if (supplements.length() > 2) {
			sb.append(" ").append(supplements.toString());
		}
		return sb.toString();
	}

	private String getFullName(PartyGroupName pgn) {
		StringBuilder sb = new StringBuilder();
		sb.append(pgn.getPrimaryPart());
		if (StringUtils.isNotBlank(pgn.getSecondaryPart())) {
			sb.append(", ").append(pgn.getSecondaryPart());
		}
		var supplements = new StringJoiner(" : ", "(", ")");
		if (StringUtils.isNotBlank(pgn.getGeneralSupplement())) {
			supplements.add(pgn.getGeneralSupplement());
		}
		if (StringUtils.isNotBlank(pgn.getChronologicalSupplement())) {
			supplements.add(pgn.getChronologicalSupplement());
		}
		if (StringUtils.isNotBlank(pgn.getGeoSupplement())) {
			supplements.add(pgn.getGeoSupplement());
		}
		if (supplements.length() > 2) {
			sb.append(" ").append(supplements.toString());
		}
		return sb.toString();
	}

	private String getFullName(PersonName personName) {
		StringBuilder sb = new StringBuilder();
		sb.append(personName.getPrimaryPart());
		if (StringUtils.isNotBlank(personName.getSecondaryPart())) {
			sb.append(", ").append(personName.getSecondaryPart());
		}

		if (StringUtils.isNotBlank(personName.getDegreeBefore())) {
			sb.append(", ").append(personName.getDegreeBefore());
		}
		if (StringUtils.isNotBlank(personName.getDegreeAfter())) {
			sb.append(", ").append(personName.getDegreeAfter());
		}
		var supplements = new StringJoiner(" : ", "(", ")");
		if (StringUtils.isNotBlank(personName.getGeneralSupplement())) {
			supplements.add(personName.getGeneralSupplement());
		}
		if (StringUtils.isNotBlank(personName.getChronologicalSupplement())) {
			supplements.add(personName.getChronologicalSupplement());
		}
		if (supplements.length() > 2) {
			sb.append(" ").append(supplements.toString());
		}
		return sb.toString();
	}
	
	private static Pattern DATE_PATTERN = Pattern.compile("^(\\d{1,2}).(\\d{1,2}).(\\d+)$");
	private static Pattern DATE_PATTERN_YEAR = Pattern.compile("^\\d+$");
	
	private ItemDateRange parseOriginatorDating(String date, String type) {
		var idr = new ItemDateRange();
		idr.setType(type);
		var matcher = DATE_PATTERN_YEAR.matcher(date);
		if (matcher.matches()) {			
			idr.setFmt("Y-Y");
			idr.setF(date);
			idr.setFe(false);
			idr.setTo(date);
			idr.setToe(false);
			idr.setVisible(true);
			return idr;
		}
		matcher = DATE_PATTERN.matcher(date);
		if (matcher.matches()) {
			idr.setFmt("D-D");
			idr.setF(date);
			idr.setFe(false);
			idr.setTo(date);
			idr.setToe(false);
			idr.setVisible(true);
			return idr;	
		}
		return null;
	}

	public String getUuid() {
		return uuid;
	}

}
