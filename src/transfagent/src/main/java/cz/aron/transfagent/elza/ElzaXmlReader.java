package cz.aron.transfagent.elza;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPoints;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.ElzaDataExchange;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Institution;
import cz.tacr.elza.schema.v2.Institutions;
import cz.tacr.elza.schema.v2.ObjectFactory;

public class ElzaXmlReader {

	static JAXBContext jaxbContext;
	static Schema schemaElza;

	static {
		try {
			jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			StreamSource stream = new StreamSource(
					ApuSourceBuilder.class.getResourceAsStream("/schema/elza-schema-v2.xsd"));
			schemaElza = schemaFactory.newSchema(stream);
		} catch (JAXBException | SAXException e) {
			throw new RuntimeException("Failed to initialize ElzaXmlReader", e);
		}
	}

	Map<String, AccessPoint> apMap = null;

	final ElzaDataExchange edx;

	public static ElzaXmlReader read(InputStream is) throws JAXBException {
		JAXBElement<ElzaDataExchange> edxElem = read(is, ElzaDataExchange.class);
		ElzaDataExchange edx = edxElem.getValue();
		return new ElzaXmlReader(edx);
	}

	public static <T> JAXBElement<T> read(InputStream is, Class<T> declaredType) throws JAXBException {
		Unmarshaller unm = jaxbContext.createUnmarshaller();
		unm.setSchema(schemaElza);
		Source source = new StreamSource(is);
		return unm.unmarshal(source, declaredType);
	}

	public static String getStringType(Fragment frg, String itemType) {
		for (DescriptionItem item : frg.getDdOrDoOrDp()) {
			if (item.getT().equals(itemType)) {
				if (item instanceof DescriptionItemString) {
					DescriptionItemString dis = (DescriptionItemString) item;
					return dis.getV();
				} else {
					throw new RuntimeException(
							"Failed to extract String value from: " + itemType + ", real type is: " + item);
				}
			}
		}
		return null;
	}

	public static AccessPoint findAccessPoint(ElzaDataExchange edx, String paid) {
		AccessPoints aps = edx.getAps();
		if (aps == null) {
			return null;
		}
		for (AccessPoint ap : aps.getAp()) {
			AccessPointEntry ape = ap.getApe();
			if (ape != null) {
				if (ape.getUuid() != null && ape.getUuid().equals(paid)) {
					return ap;
				}
			}
		}
		return null;
	}

	public static String getFullName(Fragment frg) {
		StringBuilder sb = new StringBuilder();
		sb.append(getStringType(frg, "NM_MAIN"));
		// TODO: add other name parts
		return sb.toString();
	}

	public ElzaXmlReader(final ElzaDataExchange edx) {
		this.edx = edx;
	}

	public Map<String, AccessPoint> getApMap() {
		if (apMap == null) {
			AccessPoints aps = edx.getAps();
			if (aps == null) {
				apMap = Collections.emptyMap();
			} else {
				apMap = new HashMap<>();
				for (AccessPoint ap : aps.getAp()) {
					apMap.put(ap.getApe().getId(), ap);
				}

			}
		}

		return apMap;
	}

	public ElzaDataExchange getEdx() {
		return edx;
	}

	public AccessPoint findAccessPointByUUID(String apUuid) {
		return findAccessPoint(edx, apUuid);
	}

	public Institution findInstitution(String instCode) {
		Institutions inss = edx.getInss();
		if (inss == null) {
			return null;
		}
		for (Institution inst : inss.getInst()) {
			if (inst.getC().equals(instCode)) {
				return inst;
			}
		}
		return null;
	}

}
