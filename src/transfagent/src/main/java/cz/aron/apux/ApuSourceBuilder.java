package cz.aron.apux;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuList;
import cz.aron.apux._2020.ApuSource;
import cz.aron.apux._2020.ApuType;
import cz.aron.apux._2020.Daos;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.ItemEnum;
import cz.aron.apux._2020.ItemRef;
import cz.aron.apux._2020.ItemString;
import cz.aron.apux._2020.Part;
import cz.aron.apux._2020.Parts;
import cz.aron.transfagent.transformation.CoreTypes;

public class ApuSourceBuilder {
		
	static private ApuSource apusrc = ApuxFactory.getObjFactory().createApuSource();
	
    /**
     * Collection of referenced entities
     */
    private Set<UUID> referencedEntities = new HashSet<>();
    
    public void setUuid(UUID uuid) {
        if(apusrc.getUuid()!=null) {
            throw new IllegalStateException("UUID alredy set");
        }
        apusrc.setUuid(uuid.toString());
    }

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
		return createApu(name, apuType, null);
	}

    public Apu createApu(String name, ApuType apuType, UUID apuUuid) {
        ApuList apuList = apusrc.getApus();
        if(apuList==null) {
            apuList = ApuxFactory.getObjFactory().createApuList();
            apusrc.setApus(apuList);
        }
        Apu apu = ApuxFactory.getObjFactory().createApu();
        apu.setName(name);
        apu.setType(apuType);
        if(apuUuid==null) {
            apu.setUuid(UUID.randomUUID().toString());
        } else {
            apu.setUuid(apuUuid.toString());
        }
        apuList.getApu().add(apu);
        return apu;
    }

    static public void addPart(Apu apu, Part part) {
		Parts prts = apu.getPrts();
		if(prts==null) {
			prts = ApuxFactory.getObjFactory().createParts();
			apu.setPrts(prts);
		}
		
		prts.getPart().add(part);
	}
	
	static public Part addPart(Apu apu, String partType) {
		Part part = ApuxFactory.getObjFactory().createPart();
		part.setType(partType);
		part.setItms(ApuxFactory.getObjFactory().createDescItems());
		
		addPart(apu, part);
		
		return part;
	}

	static public ItemString addString(Part part, String itemType, String value) {
		ItemString itmStr = ApuxFactory.getObjFactory().createItemString();
		itmStr.setType(itemType);
		itmStr.setValue(value);
		
		part.getItms().getStrOrLnkOrEnm().add(itmStr);
		return itmStr;
	}

	public ItemRef addApuRef(Part part, String itemType, UUID value) {
		ItemRef item = ApuxFactory.getObjFactory().createItemRef();
		item.setType(itemType);
		item.setValue(value.toString());
		
		part.getItms().getStrOrLnkOrEnm().add(item);
		
		referencedEntities.add(value);
		return item;
	}

	public List<ItemRef> addApuRefsFirstVisible(Part part, String itemType, List<UUID> uuids) {	    
	    var itemRefs = new ArrayList<ItemRef>();
	    var first = true;
	    for(var uuid:uuids) {
	        var itemRef = addApuRef(part,itemType,uuid);
	        itemRef.setVisible(first);
	        if (first) {
	            first = false;
	        }
	    }
	    return itemRefs;
	}
	
	public Set<UUID> getReferencedEntities() {
        return referencedEntities;
    }

    public Part addName(Apu apu, String name) {
		Part part = addPart(apu, CoreTypes.PT_NAME);
		addString(part, CoreTypes.NAME, name);
		part.setValue(name);
		return part;
	}

	static public void addDateRange(Part part, ItemDateRange idr) {
		part.getItms().getStrOrLnkOrEnm().add(idr);		
	}

	static public ItemDateRange createDateRange(String targetType, 
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
	
	static public ItemEnum createEnum(String targetType, String value, boolean visible) {
		ItemEnum ie = ApuxFactory.getObjFactory().createItemEnum();
		ie.setType(targetType);
		ie.setValue(value);
		ie.setVisible(visible);
		return ie;
	}

	static public ItemEnum addEnum(Part part, String targetType, String value, boolean visible) {
		ItemEnum ie = createEnum(targetType, value, visible);
		addEnum(part, ie);
		return ie;
	}
	
	static public void addEnum(Part part, ItemEnum ie) {
        part.getItms().getStrOrLnkOrEnm().add(ie);        
	}	

	static public void addDao(Apu apu, String daoId) {
		Daos daos = apu.getDaos();
		if(daos==null) {
			daos = ApuxFactory.getObjFactory().createDaos();
		}
		daos.getUuid().add(daoId);
	}

    static public List<ItemDateRange> getItemDateRanges(Apu apu, String partType, String itemType) {
        List<ItemDateRange> items = new ArrayList<>();
        for(Part part : apu.getPrts().getPart()) {
            if(part.getType().equals(partType)) {
                for(Object obj : part.getItms().getStrOrLnkOrEnm()) {
                    if(obj instanceof ItemDateRange) {
                        ItemDateRange idr = (ItemDateRange) obj;
                        if(idr.getType().equals(itemType)) {
                            items.add(idr);
                        }
                    }
                }
            }
        }
        return items;
    }
    
    static public List<ItemEnum> getItemEnums(Apu apu, ApuType partType, String itemType) {
        List<ItemEnum> items = new ArrayList<>();
        for(Part part : apu.getPrts().getPart()) {
            if(part.getType().equals(partType)) {
                for(Object obj : part.getItms().getStrOrLnkOrEnm()) {
                    if(obj instanceof ItemEnum) {
                        ItemEnum ie = (ItemEnum) obj;
                        if(ie.getType().equals(itemType)) {
                            items.add(ie);
                        }
                    }
                }
            }
        }
        return items;
    }
    

    /**
     * Return first part of given type
     * @param apu
     * @param partType
     * @return
     */
    static public Part getFirstPart(Apu apu, String partType) {
        for(Part part : apu.getPrts().getPart()) {
            if (part.getType().equals(partType)) {
                return part;
            }
        }
        return null;
    }

    static public void copyDateRanges(Part part, List<ItemDateRange> ranges) {
        for(var dateRange: ranges) {
            ItemDateRange idr = copyItem(dateRange);
            addDateRange(part, idr);
        }
        
    }

    static public void copyEnums(Part part, List<ItemEnum> itemEnums) {
        for(var itemEnum: itemEnums) {
            var ie = copyItem(itemEnum);
            addEnum(part, ie);
        }
        
    }    

    static private ItemEnum copyItem(ItemEnum itemEnum) {
        ItemEnum ie = createEnum(itemEnum.getType(), 
                                 itemEnum.getValue(), 
                                 itemEnum.isVisible());
        return ie;
    }

    static public ItemDateRange copyItem(ItemDateRange dateRange) {
        return createDateRange(dateRange.getType(), 
                             dateRange.getF(), dateRange.isFe(), 
                             dateRange.getTo(), dateRange.isToe(), 
                             dateRange.getFmt());
    }

    static public void removeItem(Apu apu, Object item) {
        for (Part part : apu.getPrts().getPart()) {
            var objects = part.getItms().getStrOrLnkOrEnm();
            for (Object obj : objects) {
                if (obj == item) {
                    objects.remove(obj);
                    break;
                }
            }
        }
    }

}
