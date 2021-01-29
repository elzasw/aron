package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public class EdxStringSpecConvertor implements EdxItemConvertor {
    
    final Map<String, String> targetTypeMap;
    Map<String, String> hiddenTargetTypeMap;

    public EdxStringSpecConvertor(final Map<String, String> targetTypeMap) {
        this.targetTypeMap = targetTypeMap; 
    }

    @Override
    public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
        DescriptionItemString itemString = (DescriptionItemString)item;
        var spec = itemString.getS();
        if(spec==null) {
            throw new RuntimeException("Missing specification, type: "+itemString.getT() 
                + ", value: "+itemString.getV());
        }
        
        String targetType = targetTypeMap.get(spec);
        if(targetType==null) {
            throw new RuntimeException("Missing mapping for specification: " + spec
                    + ", type: "+itemString.getT() 
                    + ", value: "+itemString.getV());            
        }
        ApuSourceBuilder.addString(ctx.getActivePart(), targetType, itemString.getV());
        
        // store extra hidden indexes
        if(hiddenTargetTypeMap!=null) {
            var htt= hiddenTargetTypeMap.get(spec);
            if(htt!=null) {
                var result = ApuSourceBuilder.addString(ctx.getActivePart(), htt, itemString.getV());
                result.setVisible(false);
            }
        }
        
    }

    public EdxStringSpecConvertor addIndexedItem(Map<String, String> otherIdIndexMap) {
        this.hiddenTargetTypeMap = otherIdIndexMap;
        return this;
    }

}
