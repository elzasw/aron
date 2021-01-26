package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ItemDateRange;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.TimeInterval;

public class EdxUnitDateConvertorEnum implements EdxItemConvertor {

    final private Map<String, String> dateSpecMap;
    private Map<String, String> dateOtherMapIndex;

    public EdxUnitDateConvertorEnum(final Map<String, String> dateSpecMap, 
                                    final Map<String, String> dateOtherMapIndex) {
        this.dateSpecMap = dateSpecMap;
        this.dateOtherMapIndex = dateOtherMapIndex;
    }

    @Override
    public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
        DescriptionItemUnitDate unitDate = (DescriptionItemUnitDate)item;
        
        if(unitDate.getS()==null) {
            throw new IllegalStateException("Missing specification, type: "+item.getT());
        }
        
        String targetType = dateSpecMap.get(unitDate.getS());
        if(targetType==null) {
            throw new IllegalStateException("Missing specification mapping, type: "+item.getT() + ", spec: " + item.getS());
        }
        
        ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
        TimeInterval ti = unitDate.getD();
        ItemDateRange idr = apusBuilder.createDateRange(targetType, ti.getF(), ti.isFe(), 
                ti.getTo(), ti.isToe(), ti.getFmt());
        apusBuilder.addDateRange(ctx.getActivePart(), idr);
        
        // skryta indexace
        if(dateOtherMapIndex!=null) {
            String indexType = dateOtherMapIndex.get(unitDate.getS());
            if(indexType!=null) {
                var indexItem = apusBuilder.copyItem(idr);
                indexItem.setVisible(false);
                indexItem.setType(indexType);
                apusBuilder.addDateRange(ctx.getActivePart(), indexItem);
            }
        }
    }

}
