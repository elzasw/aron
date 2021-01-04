package cz.aron.apux;

import java.io.OutputStream;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuList;
import cz.aron.apux._2020.ApuSource;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.ItemEnum;
import cz.aron.apux._2020.ItemRef;
import cz.aron.apux._2020.ItemString;
import cz.aron.apux._2020.Part;
import cz.aron.apux._2020.Parts;
import cz.aron.common.itemtypes.TypesConfiguration;
import cz.aron.transfagent.transformation.CoreTypes;

public class ApuSourceBuilder {
		
	private ApuSource apusrc = ApuxFactory.getObjFactory().createApuSource();

	public ApuSource getApusrc() {
		return apusrc;
	}

	public JAXBElement<ApuSource> build() {
		if(apusrc.getUuid()==null) {
			apusrc.setUuid(UUID.randomUUID().toString());
		}
		JAXBElement<ApuSource> result = ApuxFactory.getObjFactory().createApusrc(apusrc);
		return result;
	}

    public void build(OutputStream fos, ApuValidator validator) throws JAXBException {
        if (validator != null) {
            validator.validate(apusrc);
        }
        build(fos);
    }

	public void build(OutputStream fos) throws JAXBException {
		JAXBElement<ApuSource> apusrc = build();
		Marshaller marshaller = ApuxFactory.createMarshaller();
		marshaller.marshal(apusrc, fos);
	}

	public Apu createApu(String name, ApuType apuType) {
		ApuList apuList = apusrc.getApus();
		if(apuList==null) {
			apuList = ApuxFactory.getObjFactory().createApuList();
			apusrc.setApus(apuList);
		}
		Apu apu = ApuxFactory.getObjFactory().createApu();
		apu.setName(name);
		apu.setType(apuType);
		apu.setUuid(UUID.randomUUID().toString());
		apuList.getApu().add(apu);
		return apu;
	}

	public void addPart(Apu apu, Part part) {
		Parts prts = apu.getPrts();
		if(prts==null) {
			prts = ApuxFactory.getObjFactory().createParts();
			apu.setPrts(prts);
		}
		
		prts.getPart().add(part);
	}
	
	public Part addPart(Apu apu, String partType) {
		Part part = ApuxFactory.getObjFactory().createPart();
		part.setType(partType);
		part.setItms(ApuxFactory.getObjFactory().createDescItems());
		
		addPart(apu, part);
		
		return part;
	}

	public ItemString addString(Part part, String itemType, String value) {
		ItemString itmStr = ApuxFactory.getObjFactory().createItemString();
		itmStr.setType(itemType);
		itmStr.setValue(value);
		
		part.getItms().getStrOrLnkOrEnm().add(itmStr);
		return itmStr;
	}

	public ItemRef addApuRef(Part part, String itemType, String value) {
		ItemRef item = ApuxFactory.getObjFactory().createItemRef();
		item.setType(itemType);
		item.setValue(value);
		
		part.getItms().getStrOrLnkOrEnm().add(item);
		return item;
	}

	public Part addName(Apu apu, String name) {
		Part part = addPart(apu, CoreTypes.PT_NAME);
		addString(part, CoreTypes.NAME, name);
		part.setValue(name);
		return part;
	}

	public void addDateRange(Part part, ItemDateRange idr) {
		part.getItms().getStrOrLnkOrEnm().add(idr);		
	}

	public ItemDateRange createDateRange(String targetType, 
			String from, Boolean fromEst, String to, Boolean toEst, String format) {
		ItemDateRange idr = ApuxFactory.getObjFactory().createItemDateRange();
		idr.setType(targetType);
		idr.setF(from);
		idr.setFe(fromEst);
		idr.setTo(to);
		idr.setToe(toEst);
		idr.setFmt(format);
		return idr;
	}
	
	public ItemEnum createEnum(String targetType, String value, boolean visible) {
		ItemEnum ie = ApuxFactory.getObjFactory().createItemEnum();
		ie.setType(targetType);
		ie.setValue(value);
		ie.setVisible(visible);
		return ie;
	}

	public ItemEnum addEnum(Part aeInfoPart, String targetType, String value, boolean visible) {
		ItemEnum ie = createEnum(targetType, value, visible);
		aeInfoPart.getItms().getStrOrLnkOrEnm().add(ie);
		return ie;
	}


}
