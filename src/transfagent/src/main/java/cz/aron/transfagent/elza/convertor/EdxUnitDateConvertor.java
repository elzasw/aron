package cz.aron.transfagent.elza.convertor;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.ItemDateRange;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemUnitDate;
import cz.tacr.elza.schema.v2.TimeInterval;

public class EdxUnitDateConvertor implements EdxItemConvertor {
	
	final private String targetType;

	public EdxUnitDateConvertor(final String targetType) {
		this.targetType = targetType;
	}

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		DescriptionItemUnitDate unitDate = (DescriptionItemUnitDate)item;
		
		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
		TimeInterval ti = unitDate.getD();
		ItemDateRange idr = apusBuilder.createDateRange(targetType, ti.getF(), ti.isFe(), 
				ti.getTo(), ti.isToe(), ti.getFmt());
		apusBuilder.addDateRange(ctx.getActivePart(), idr);

	}

}
