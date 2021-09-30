package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetFindingAidResponse;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.GetOriginatorResponse;
import cz.aron.peva2.wsdl.NadPrimarySheet;
import cz.aron.peva2.wsdl.NadSheet;
import cz.aron.peva2.wsdl.NadSubsheet;
import cz.aron.peva2.wsdl.ObjectFactory;

public class Peva2XmlReader {
    
    private static Logger log = LoggerFactory.getLogger(Peva2XmlReader.class);

    private static JAXBContext JAXB_CONTEXT;
    
    protected static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(ObjectFactory.class);
        } catch (JAXBException e) {
            log.error("Failed to initialize PEvA 2 schema", e);
            throw new RuntimeException("Failed to initialize Peva2Reader", e);
        }
    }

    /**
     * Nacte NadSheet z xml souboru
     * 
     * @param path
     *            cesta k xml souboru
     * @return NadSheet
     * @throws JAXBException
     * @throws IOException
     */
    public static NadSheet unmarshalNadSheet(Path path) throws JAXBException, IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return (NadSheet) JAXB_CONTEXT.createUnmarshaller().unmarshal(is);
        }
    }
    
    public static GetNadSheetResponse unmarshalGetNadSheetResponse(Path path) throws IOException, JAXBException {
    	try (InputStream is = Files.newInputStream(path)) {
            return ((JAXBElement<GetNadSheetResponse>) JAXB_CONTEXT.createUnmarshaller().unmarshal(is)).getValue();
        }
    }

	public static void marshalGetNadSheetResponse(GetNadSheetResponse gnsr, Path fileName) throws JAXBException {						
		JAXB_CONTEXT.createMarshaller().marshal(OBJECT_FACTORY.createGetNadSheetResponse(gnsr), fileName.toFile());
	}
	
	public static void marshalGetFindingAidResponse(GetFindingAidResponse gfar, Path fileName) throws JAXBException {
		JAXB_CONTEXT.createMarshaller().marshal(OBJECT_FACTORY.createGetFindingAidResponse(gfar), fileName.toFile());
	}

	public static GetFindingAidResponse unmarshalGetFindingAidResponse(Path path) throws IOException, JAXBException {
		try (InputStream is = Files.newInputStream(path)) {
			return ((JAXBElement<GetFindingAidResponse>) JAXB_CONTEXT.createUnmarshaller().unmarshal(is)).getValue();
		}
	}
	
	public static void marshalGetOriginatorResponse(GetOriginatorResponse gfar, Path fileName) throws JAXBException {
		JAXB_CONTEXT.createMarshaller().marshal(OBJECT_FACTORY.createGetOriginatorResponse(gfar), fileName.toFile());
	}

	public static GetOriginatorResponse unmarshalGetOriginatorResponse(Path path) throws IOException, JAXBException {
		try (InputStream is = Files.newInputStream(path)) {
			return ((JAXBElement<GetOriginatorResponse>) JAXB_CONTEXT.createUnmarshaller().unmarshal(is)).getValue();
		}
	}	

	
}
