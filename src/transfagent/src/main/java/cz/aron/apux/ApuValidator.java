package cz.aron.apux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import cz.aron.apux._2020.Apu;
import cz.aron.apux._2020.ApuSource;
import cz.aron.apux._2020.ItemDateRange;
import cz.aron.apux._2020.ItemEnum;
import cz.aron.apux._2020.ItemLink;
import cz.aron.apux._2020.ItemRef;
import cz.aron.apux._2020.ItemString;
import cz.aron.apux._2020.Part;
import cz.aron.common.itemtypes.ItemTypeConfig;
import cz.aron.common.itemtypes.ItemTypeConfig.Types;
import cz.aron.common.itemtypes.PartTypeConfig;
import cz.aron.common.itemtypes.TypesConfiguration;

public class ApuValidator {

    Map<String, PartTypeConfig> mapParts = new HashMap<>();
    Map<String, Types> mapItems = new HashMap<>();

    public ApuValidator(TypesConfiguration config) {
        List<PartTypeConfig> partTypes = config.getPartTypes();
        for (PartTypeConfig item : partTypes) {
            mapParts.put(item.getCode(), item);
        }
        List<ItemTypeConfig> itemTypes = config.getItemTypes();
        for (ItemTypeConfig item : itemTypes) {
            mapItems.put(item.getCode(), item.getType());
        }
    }

    public void validate(ApuSource apusrc) throws JAXBException {
        List<Apu> apus = apusrc.getApus().getApu();
        for (Apu apu : apus) {
            for (Part part : apu.getPrts().getPart()) {
                if (mapParts.get(part.getType()) == null) {
                    throw new IllegalStateException("Illegal part: " + part.getType());
                }
                for (Object obj : part.getItms().getStrOrLnkOrEnm()) {
                    validateItem(obj);
                }
            }
        }
    }

    private void validateItem(Object obj) {
        // ItemString
        if (obj instanceof ItemString) {
            validateItem(((ItemString) obj).getType(), Types.STRING);
        }
        // ItemLink
        if (obj instanceof ItemLink) {
            validateItem(((ItemLink) obj).getType(), Types.LINK);
        }
        // ItemEnum
        if (obj instanceof ItemEnum) {
            ItemEnum item = (ItemEnum) obj;
            validateItem(((ItemEnum) obj).getType(), Types.ENUM);
        }
        // ItemRef
        if (obj instanceof ItemRef) {
            validateItem(((ItemRef) obj).getType(), Types.APU_REF);
        }
        // ItemDateRange
        if (obj instanceof ItemDateRange) {
            validateItem(((ItemDateRange) obj).getType(), Types.UNITDATE);
        }
    }

    public void validateItem(String itemType, Types value) {
        Types type = mapItems.get(itemType);
        if (type == null ) {
            throw new IllegalStateException("Item type not found in definition, item type: "+itemType);            
        }
        if( type != value) {
            throw new IllegalStateException("Wrong type of item type: " + itemType + ", expected: " + type + ", received: "+ value);
        }
    }

    public Map<String, Types> getMapItems() {
        return mapItems;
    }

}
