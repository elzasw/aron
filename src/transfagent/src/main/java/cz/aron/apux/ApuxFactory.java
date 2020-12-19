package cz.aron.apux;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import cz.aron.apux._2020.ObjectFactory;

public class ApuxFactory {
    
    private static ObjectFactory objFactory = new ObjectFactory();

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

    public static Marshaller createMarshaller() throws JAXBException {
        Marshaller marshaller = apuxXmlContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.setSchema(schemaApux);
        return marshaller;
    }

    public static Unmarshaller createUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = apuxXmlContext.createUnmarshaller();
        unmarshaller.setSchema(schemaApux);
        return unmarshaller;
    }

    public static ObjectFactory getObjFactory() {
        return objFactory;
    }

}
