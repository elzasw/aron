package cz.aron.transfagent.elza.convertor;

import java.util.ArrayList;
import java.util.List;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public class EdxStringConvertor implements EdxItemConvertor {

	final private String targetType;
	
	List<String> hiddenTargetTypes;

	public EdxStringConvertor(final String targetType) {
		this.targetType = targetType;
	}

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		DescriptionItemString itemString = (DescriptionItemString)item;
		
		ApuSourceBuilder.addString(ctx.getActivePart(), targetType, itemString.getV());
		
		// store extra hidden indexes
		if(hiddenTargetTypes!=null) {
		    for(String htt: hiddenTargetTypes) {
		        var result = ApuSourceBuilder.addString(ctx.getActivePart(), htt, itemString.getV());
		        result.setVisible(false);
		    }
		}
	}

    public EdxStringConvertor addIndexedItem(String unitIdIndexed) {
        if(hiddenTargetTypes==null) {
            hiddenTargetTypes = new ArrayList<>(1);
        }
        hiddenTargetTypes.add(unitIdIndexed);
        return this;
    }

}
