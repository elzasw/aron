package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.elza.ElzaTypes;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.StructuredObject;

public class EdxStorageConvertor implements EdxItemConvertor {
	
	private final String targetType;
    
    private final Map<String, StructuredObject> soMap;

    public EdxStorageConvertor(final String targetType, 
                                 final Map<String, StructuredObject> soMap) {
        this.targetType = targetType;
        this.soMap = soMap;
    }


	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		DescriptionItemStructObjectRef itemSoRef = (DescriptionItemStructObjectRef)item;
        var soId = itemSoRef.getSoid();
        var so = soMap.get(soId);
        if(so==null) {
            throw new IllegalStateException("Missing structured object, soId: "+soId);
        }
        // Convert structured object to text
        String value = so.getV();
        if(StringUtils.isEmpty(value)) {
            // only items with value can be exported
            return;
        }
        var lvl = ctx.getProcessedLevel();
        if (lvl!=null) {
        	for(var descItem:lvl.getDdOrDoOrDp()) {
        		if (ElzaTypes.ZP2015_ITEM_ORDER.equals(descItem.getT())) {
        			var itemInt = (DescriptionItemInteger)descItem;
        			var v = itemInt.getV();
        			if (v!=null) {
        				value = value + " / " + v.toString();
        			}
        		}
        	}
        }                
        ApuSourceBuilder.addString(ctx.getActivePart(), targetType, value);		
	}

}
