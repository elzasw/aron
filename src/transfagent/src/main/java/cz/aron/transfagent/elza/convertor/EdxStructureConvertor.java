package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemStructObjectRef;
import cz.tacr.elza.schema.v2.StructuredObject;

public class EdxStructureConvertor implements EdxItemConvertor {
    
    final private String targetType;
    
    final private Map<String, StructuredObject> soMap;

    public EdxStructureConvertor(final String targetType, 
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
        ApuSourceBuilder.addString(ctx.getActivePart(), targetType, value);
    }

}
