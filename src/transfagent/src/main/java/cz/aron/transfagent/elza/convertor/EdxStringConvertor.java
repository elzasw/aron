package cz.aron.transfagent.elza.convertor;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemString;

public class EdxStringConvertor implements EdxItemConvertor {

	final private String targetType;

	public EdxStringConvertor(final String targetType) {
		this.targetType = targetType;
	}

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		DescriptionItemString itemString = (DescriptionItemString)item;
		
		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
		apusBuilder.addString(ctx.getActivePart(), targetType, itemString.getV());
	}

}
