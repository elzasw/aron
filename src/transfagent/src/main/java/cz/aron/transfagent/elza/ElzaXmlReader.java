package cz.aron.transfagent.elza;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.ElzaDataExchange;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class ElzaXmlReader {

	static JAXBContext jaxbContext;
	static Schema schemaElza;
	
	static {
		try {
			jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            StreamSource stream = new StreamSource(ApuSourceBuilder.class.getResourceAsStream("/schema/elza-schema-v2.xsd"));
            schemaElza = schemaFactory.newSchema(stream);						
		} catch (JAXBException | SAXException e) {
			throw new RuntimeException("Failed to initialize ElzaXmlReader", e); 
		}
	}

	public static <T> JAXBElement<T> read(InputStream is, Class<T> declaredType) throws JAXBException {
		Unmarshaller unm = jaxbContext.createUnmarshaller();
		unm.setSchema(schemaElza);
		Source source = new StreamSource(is);
		return unm.unmarshal(source, declaredType);
	}

	public String getStringType(Fragment frg, String itemType) {
		for(DescriptionItem item: frg.getDdOrDoOrDp()) {
			if(item.getT().equals(itemType)) {
				if(item instanceof DescriptionItemString) {
					DescriptionItemString dis = (DescriptionItemString)item;
					return dis.getV();
				} else {
					throw new RuntimeException("Failed to extract String value from: "+itemType + 
							", real type is: " + item);
				}
			}
		}
		return null;
	}

}
