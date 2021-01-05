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
import cz.aron.common.itemtypes.TypesConfiguration;

public class ApuValidator {

    Map<String, Types> mapItems = new HashMap<>();

    public ApuValidator(TypesConfiguration config) {
        List<ItemTypeConfig> itemTypes = config.getItemTypes();
        for (ItemTypeConfig item : itemTypes) {
            mapItems.put(item.getCode(), item.getType());
        }
    }

    public void validate(ApuSource apusrc) throws JAXBException {
        List<Apu> apus = apusrc.getApus().getApu();
        for (Apu apu : apus) {
            for (Part part : apu.getPrts().getPart()) {
                for (Object obj : part.getItms().getStrOrLnkOrEnm()) {
                    validateItem(obj);
                }
            }
        }
    }

    private void validateItem(Object obj) {
        // ItemString
        if (obj instanceof ItemString) {
            ItemString item = (ItemString) obj;
            Types type = mapItems.get(item.getType());
            if (type == null || type != Types.STRING) {
                throw new IllegalStateException();
            }
        }
        // ItemLink
        if (obj instanceof ItemLink) {
            ItemLink item = (ItemLink) obj;
            Types type = mapItems.get(item.getType());
            if (type == null || type != Types.LINK) {
                throw new IllegalStateException();
            }
        }
        // ItemEnum
        if (obj instanceof ItemEnum) {
            ItemEnum item = (ItemEnum) obj;
            Types type = mapItems.get(item.getType());
            if (type == null || type != Types.ENUM) {
                throw new IllegalStateException();
            }
        }
        // ItemRef
        if (obj instanceof ItemRef) {
            ItemRef item = (ItemRef) obj;
            Types type = mapItems.get(item.getType());
            if (type == null || type != Types.APU_REF) {
                throw new IllegalStateException();
            }
        }
        // ItemDateRange
        if (obj instanceof ItemDateRange) {
            ItemDateRange item = (ItemDateRange) obj;
            Types type = mapItems.get(item.getType());
            if (type == null || type != Types.UNITDATE) {
                throw new IllegalStateException();
            }
        }
    }

    public Map<String, Types> getMapItems() {
        return mapItems;
    }

}
