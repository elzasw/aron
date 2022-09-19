package cz.aron.transfagent.elza.convertor;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;

public class EdxIntConvertor implements EdxItemConvertor {
    
    private final String targetType;

    public EdxIntConvertor(final String targetType) {
        this.targetType = targetType;
    }

    @Override
    public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
        DescriptionItemInteger itemInt = (DescriptionItemInteger)item;

        StringBuilder sb = new StringBuilder();        
        sb.append(itemInt.getV().toString());
        
        ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
        apusBuilder.addString(ctx.getActivePart(), targetType, sb.toString());        
    }


}
