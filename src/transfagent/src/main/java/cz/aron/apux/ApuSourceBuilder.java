package cz.aron.apux;

import java.io.OutputStream;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuList;
import cz.aron.apux._2020.ApuSource;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.ItemRef;
import cz.aron.apux._2020.ItemString;
import cz.aron.apux._2020.ObjectFactory;
import cz.aron.apux._2020.Part;
import cz.aron.apux._2020.Parts;

public class ApuSourceBuilder {
	
	static ObjectFactory objFactory = new ObjectFactory();
	
    public static JAXBContext apuxXmlContext;
	
    public static Schema schemaApux;
	
	static {
		try {			
			apuxXmlContext = JAXBContext.newInstance(ObjectFactory.class);
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            StreamSource stream = new StreamSource(ApuSourceBuilder.class.getResourceAsStream("/wsdl/aron_apux.xsd"));
            schemaApux = schemaFactory.newSchema(stream);			
		} catch (JAXBException | SAXException e) {
			throw new RuntimeException("Failed to initialize ApuSourceBuilder", e); 
		} 
		
	}
		
	private ApuSource apusrc = objFactory.createApuSource();

	public JAXBElement<ApuSource> build() {
		if(apusrc.getUuid()==null) {
			apusrc.setUuid(UUID.randomUUID().toString());
		}
		JAXBElement<ApuSource> result = objFactory.createApusrc(apusrc);
		return result;
	}

	public void build(OutputStream fos) throws JAXBException {
		JAXBElement<ApuSource> apusrc = build();
		Marshaller marshaller = apuxXmlContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setSchema(schemaApux);
		marshaller.marshal(apusrc, fos);
	}

	public Apu createApu(String name, ApuType apuType) {
		ApuList apuList = apusrc.getApus();
		if(apuList==null) {
			apuList = objFactory.createApuList();
			apusrc.setApus(apuList);
		}
		Apu apu = objFactory.createApu();
		apu.setName(name);
		apu.setType(apuType);
		apu.setUuid(UUID.randomUUID().toString());
		apuList.getApu().add(apu);
		return apu;
	}

	public Part addPart(Apu apu, String partType) {
		Parts prts = apu.getPrts();
		if(prts==null) {
			prts = objFactory.createParts();
			apu.setPrts(prts);
		}
		Part part = objFactory.createPart();
		part.setType(partType);
		part.setItms(objFactory.createDescItems());
		
		prts.getPart().add(part);
		
		return part;
	}

	public ItemString addString(Part part, String itemType, String value) {
		ItemString itmStr = objFactory.createItemString();
		itmStr.setType(itemType);
		itmStr.setValue(value);
		
		part.getItms().getStrOrLnkOrEnm().add(itmStr);
		return itmStr;
	}

	public ItemRef addApuRef(Part part, String itemType, String value) {
		ItemRef item = objFactory.createItemRef();
		item.setType(itemType);
		item.setValue(value);
		
		part.getItms().getStrOrLnkOrEnm().add(item);
		return item;
	}

	public Part addName(Apu apu, String name) {
		Part part = addPart(apu, "PT_NAME");
		addString(part, "NAME", name);
		part.setValue(name);
		return part;
	}

	public void addDateRange(Part part, ItemDateRange idr) {
		part.getItms().getStrOrLnkOrEnm().add(idr);		
	}

	public ItemDateRange createDateRange(String targetType, 
			String from, Boolean fromEst, String to, Boolean toEst, String format) {
		ItemDateRange idr = this.objFactory.createItemDateRange();
		idr.setType(targetType);
		idr.setF(from);
		idr.setFe(fromEst);
		idr.setTo(to);
		idr.setToe(toEst);
		idr.setFmt(format);
		return idr;
	}

}
