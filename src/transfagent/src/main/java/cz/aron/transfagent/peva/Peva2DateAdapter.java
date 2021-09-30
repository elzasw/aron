package cz.aron.transfagent.peva;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class Peva2DateAdapter extends XmlAdapter<String, XMLGregorianCalendar> {

	@Override
	public XMLGregorianCalendar unmarshal(String v) throws Exception {
		XMLGregorianCalendar result;
		try {
			result = DatatypeFactory.newInstance().newXMLGregorianCalendar(v);
		} catch (IllegalArgumentException iaEx) {
			GregorianCalendar gc = GregorianCalendar.from(LocalDateTime
					.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")).atZone(ZoneId.systemDefault()));
			result = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
		}
		return result;
	}

	@Override
	public String marshal(XMLGregorianCalendar v) throws Exception {						
		return v.toXMLFormat();
	}

}
