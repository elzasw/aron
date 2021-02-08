package cz.aron.transfagent.elza;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.AccessPointEntry;
import cz.tacr.elza.schema.v2.AccessPoints;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemString;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;
import cz.tacr.elza.schema.v2.ElzaDataExchange;
import cz.tacr.elza.schema.v2.Fragment;
import cz.tacr.elza.schema.v2.Institution;
import cz.tacr.elza.schema.v2.Institutions;
import cz.tacr.elza.schema.v2.ObjectFactory;
import cz.tacr.elza.schema.v2.StructuredObject;

public class ElzaXmlReader {
	
	static Logger log = LoggerFactory.getLogger(ElzaXmlReader.class);

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
			log.error("Failed to initialize Elza schema", e);
			throw new RuntimeException("Failed to initialize ElzaXmlReader", e);
		}
	}

	Map<String, AccessPoint> apMap = null;
	
	Map<String, StructuredObject> soMap = null;

	final ElzaDataExchange edx;

    public ElzaXmlReader(final ElzaDataExchange edx) {
        this.edx = edx;
    }

    public ElzaDataExchange getEdx() {
        return edx;
    }

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

	public static String getEnumValue(Fragment frg, String itemType) {
		for (DescriptionItem item : frg.getDdOrDoOrDp()) {
			if (item.getT().equals(itemType)) {
				if (item instanceof DescriptionItemEnum) {
					DescriptionItemEnum die = (DescriptionItemEnum) item;
					return die.getS();
				} else {
					throw new RuntimeException(
							"Failed to extract enum value from: " + itemType + ", real type is: " + item);
				}
			}
		}
		return null;
	}
	
	public static <T> T getItem(Fragment frg, String itemType, Class<T> tc) {
        for (DescriptionItem item : frg.getDdOrDoOrDp()) {
            if (item.getT().equals(itemType)) {
                if(item instanceof DescriptionItemUndefined) {
                    continue;
                } else
                if (tc.isAssignableFrom(item.getClass())) {
                    T result = tc.cast(item);
                    return result;
                } else {
                    throw new RuntimeException(
                            "Failed to extract: " + itemType + 
                            ", expected class: " + tc + ", real type is: " + item);
                }
            }
        }
        return null;
	    
	}
	
    public static DescriptionItemUriRef getLink(Fragment frg, String itemType) {
        return getItem(frg, itemType, DescriptionItemUriRef.class);
    }

    public static DescriptionItemAPRef getApRef(Fragment frg, String itemType) {
        return getItem(frg, itemType, DescriptionItemAPRef.class);	    
	}

	public static String getApRefId(Fragment frg, String itemType) {
	    DescriptionItemAPRef item = getApRef(frg, itemType);
	    return item!=null?item.getApid():null;
	}
	
	private static List<String> getTypes(Fragment frg, String[] types) {
		
		Set<String> itemTypes = new HashSet<>();
		for(String type: types) {
			itemTypes.add(type);
		}
		
		List<String> result = new ArrayList<>();
		
		for (DescriptionItem item : frg.getDdOrDoOrDp()) {
			if (itemTypes.contains(item.getT())) {
				if (item instanceof DescriptionItemString) {
					DescriptionItemString dis = (DescriptionItemString) item;
					result.add(dis.getV());
				} else {
					throw new RuntimeException(
							"Failed to extract String value from: " + item.getT() + ", real type is: " + item);
				}
			}
		}
		return result;
	}
	
	public static String getSingleEnum(Fragment frg, String enumType) {
		for (DescriptionItem item : frg.getDdOrDoOrDp()) {
			if (item.getT().equals(enumType)) {
				if (item instanceof DescriptionItemEnum) {
					DescriptionItemEnum die = (DescriptionItemEnum) item;
					if(die.getT().equals(enumType)) {
						return die.getS();
					}
				}
			}
		}
		return null;
	}

	public static String getStringsType(Fragment frg, String itemType, String separator) {
		StringBuilder sb = new StringBuilder();
		
		for (DescriptionItem item : frg.getDdOrDoOrDp()) {
			if (item.getT().equals(itemType)) {
				if (item instanceof DescriptionItemString) {
					DescriptionItemString dis = (DescriptionItemString) item;
					String v = dis.getV();
					if(StringUtils.isNotBlank(v)) {
						if(sb.length()>0&&separator!=null) {
							sb.append(separator);
						}
						sb.append(v);
					}
				} else {
					throw new RuntimeException(
							"Failed to extract String value from: " + itemType + ", real type is: " + item);
				}
			}
		}
		if(sb.length()>0) {
			return sb.toString();
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
		sb.append(getStringType(frg, ElzaTypes.NM_MAIN));
		
		String minor = getStringsType(frg, ElzaTypes.NM_MINOR, ", ");
		if(StringUtils.isNotEmpty(minor)) {
			sb.append(", ").append(minor);
		}
		
		StringBuilder sbTitules = new StringBuilder();
		
		String degreePre = getStringType(frg, ElzaTypes.NM_DEGREE_PRE);		
		if(StringUtils.isNotEmpty(degreePre)) {
			sbTitules.append(degreePre);
		}
		String degreePost = getStringType(frg, ElzaTypes.NM_DEGREE_POST);
		if(StringUtils.isNotBlank(degreePost)) {
			if(sbTitules.length()>0) {
				sb.append(" ");
			}
			sbTitules.append(degreePost);
		}
		
		List<String> additions = getTypes(frg, ElzaTypes.NM_SUPS);		
		if(additions.size()>0) {
			sb.append(" (");
			sb.append(String.join(" : ", additions));
			sb.append(")");
		}
		
		return sb.toString();
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
	
	public Map<String, StructuredObject> getSoMap() {
	    if(soMap==null) {
	        soMap = new HashMap<>();
	        if(edx.getFs()!=null) {
	            for(var s:edx.getFs().getS() ) {
	                if(s.getSts()!=null) {
	                    for(var st: s.getSts().getSt()) {
	                        if(st.getSos()!=null) {
	                            for(var so: st.getSos().getSo()) {
	                                soMap.put(so.getId(), so);
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	    return soMap;
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

	public AccessPoint getSingleAccessPoint() {
		AccessPoints aps = edx.getAps();
		if (aps == null) {
			return null;
		}
		if(aps.getAp().size()!=1) {
			return null;
		}
		return aps.getAp().get(0);
	}



}
