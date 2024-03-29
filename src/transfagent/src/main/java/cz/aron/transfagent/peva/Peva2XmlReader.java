package cz.aron.transfagent.peva;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.aron.peva2.wsdl.GetFindingAidAuthorResponse;
import cz.aron.peva2.wsdl.GetFindingAidCopyResponse;
import cz.aron.peva2.wsdl.GetFindingAidResponse;
import cz.aron.peva2.wsdl.GetGeoObjectResponse;
import cz.aron.peva2.wsdl.GetNadSheetResponse;
import cz.aron.peva2.wsdl.GetOriginatorResponse;
import cz.aron.peva2.wsdl.NadSheet;
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
    
    @SuppressWarnings("unchecked")
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

	@SuppressWarnings("unchecked")
	public static GetFindingAidResponse unmarshalGetFindingAidResponse(Path path) throws IOException, JAXBException {
		try (InputStream is = Files.newInputStream(path)) {
			return ((JAXBElement<GetFindingAidResponse>) JAXB_CONTEXT.createUnmarshaller().unmarshal(is)).getValue();
		}
	}
	
	public static void marshalGetOriginatorResponse(GetOriginatorResponse gor, Path fileName) throws JAXBException {
		JAXB_CONTEXT.createMarshaller().marshal(OBJECT_FACTORY.createGetOriginatorResponse(gor), fileName.toFile());
	}

	@SuppressWarnings("unchecked")
	public static GetOriginatorResponse unmarshalGetOriginatorResponse(Path path) throws IOException, JAXBException {
		try (InputStream is = Files.newInputStream(path)) {
			return ((JAXBElement<GetOriginatorResponse>) JAXB_CONTEXT.createUnmarshaller().unmarshal(is)).getValue();
		}
	}

	public static void marshalGetFindingAidCopyResponse(GetFindingAidCopyResponse gfacr, Path fileName) throws JAXBException {
		JAXB_CONTEXT.createMarshaller().marshal(OBJECT_FACTORY.createGetFindingAidCopyResponse(gfacr), fileName.toFile());
	}

	@SuppressWarnings("unchecked")
	public static GetFindingAidCopyResponse unmarshalGetFindingAidCopyResponse(Path path) throws IOException, JAXBException {
		try (InputStream is = Files.newInputStream(path)) {
			return ((JAXBElement<GetFindingAidCopyResponse>) JAXB_CONTEXT.createUnmarshaller().unmarshal(is)).getValue();
		}
	}

	public static void marshalGetGeoObjectResponse(GetGeoObjectResponse ggor, Path fileName) throws JAXBException {
		JAXB_CONTEXT.createMarshaller().marshal(OBJECT_FACTORY.createGetGeoObjectResponse(ggor), fileName.toFile());
	}

	@SuppressWarnings("unchecked")
	public static GetGeoObjectResponse unmarshalGetGeoObjectResponse(Path path) throws IOException, JAXBException {
		try (InputStream is = Files.newInputStream(path)) {
			return ((JAXBElement<GetGeoObjectResponse>) JAXB_CONTEXT.createUnmarshaller().unmarshal(is)).getValue();
		}
	}
	
	public static void marshalGetFindingAidAuthorResponse(GetFindingAidAuthorResponse gfacr, Path fileName) throws JAXBException {
		JAXB_CONTEXT.createMarshaller().marshal(OBJECT_FACTORY.createGetFindingAidAuthorResponse(gfacr), fileName.toFile());
	}

	@SuppressWarnings("unchecked")
	public static GetFindingAidAuthorResponse unmarshalGetFindingAidAuthorResponse(Path path) throws IOException, JAXBException {
		try (InputStream is = Files.newInputStream(path)) {
			return ((JAXBElement<GetFindingAidAuthorResponse>) JAXB_CONTEXT.createUnmarshaller().unmarshal(is)).getValue();
		}
	}
	
}
