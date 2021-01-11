package cz.aron.transfagent.elza.convertor;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;

public class EdxTimeLenghtConvertor implements EdxItemConvertor {
    
    final private String targetType;

    public EdxTimeLenghtConvertor(final String targetType) {
        this.targetType = targetType;
    }

    @Override
    public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
        DescriptionItemInteger itemInt = (DescriptionItemInteger)item;
        
        // TODO: Do real conversion to hh:mm:ss
        StringBuilder sb = new StringBuilder();        
        sb.append(itemInt.getV().toString());
        
        ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
        apusBuilder.addString(ctx.getActivePart(), targetType, sb.toString());        
    }

}
