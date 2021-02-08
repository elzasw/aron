package cz.aron.transfagent.ead3;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.archivists.ead3.schema.Date;
import org.archivists.ead3.schema.Ead;
import org.archivists.ead3.schema.Localcontrol;
import org.archivists.ead3.schema.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import cz.aron.apux.ApuSourceBuilder;

public class Ead3XmlReader {

    static Logger log = LoggerFactory.getLogger(Ead3XmlReader.class);

    static JAXBContext jaxbContext;
    static Schema schemaEad3;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            StreamSource stream = new StreamSource(
                    ApuSourceBuilder.class.getResourceAsStream("/schema/ead3.xsd"));
            schemaEad3 = schemaFactory.newSchema(stream);
        } catch (JAXBException | SAXException e) {
            log.error("Failed to initialize Ead3 schema", e);
            throw new RuntimeException("Failed to initialize Ead3XmlReader", e);
        }
    }

    final Ead ead;

    public Ead3XmlReader(Ead ead) {
        this.ead = ead;
    }

    public Ead getEad() {
        return ead;
    }

    public static Ead3XmlReader read(InputStream is) throws JAXBException {
        JAXBElement<Ead> eadElement = read(is, Ead.class);
        Ead ead = eadElement.getValue();
        return new Ead3XmlReader(ead);
    }

    public static <T> JAXBElement<T> read(InputStream is, Class<T> declaredType) throws JAXBException {
        Unmarshaller unm = jaxbContext.createUnmarshaller();
        unm.setSchema(schemaEad3);
        Source source = new StreamSource(is);
        return unm.unmarshal(source, declaredType);
    }

    public String getRecordId() {
        return ead.getControl().getRecordid().getContent();
    }

    public String getSubtitle() {
        var subtitules = ead.getControl().getFiledesc().getTitlestmt().getSubtitle();
        var contents = subtitules.get(0).getContent();
        return contents.get(0).toString();
    }

    public String getInstitutionCode() {
        return ead.getControl().getMaintenanceagency().getAgencycode().getContent();
    }

    public String getReleaseDatePlace() {
        for (Object obj : ead.getControl().getFiledesc().getPublicationstmt().getPublisherOrDateOrAddress()) {
            if (obj instanceof Date) {
                Date item = (Date) obj;
                if (item.getLocaltype().equals("RELEASE_DATE_PLACE")) {
                    return item.getContent().get(0).toString();
                }
            }
        }
        return null;
    }

    public String getLocalionControlByType(String localType) {
        for (Localcontrol lc : ead.getControl().getLocalcontrol()) {
            if (lc.getLocaltype().equals(localType)) {
                return lc.getTerm().getContent();
            }
        }
        return null;
    }

}
