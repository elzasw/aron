package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;

public class EdxEnumConvertor implements EdxItemConvertor {

	final private String targetType;
	final private Map<String, String> enumMap;

	public EdxEnumConvertor(final String targetType,
			final Map<String, String> enumMap) {
		this.targetType = targetType;
		this.enumMap = enumMap;
	}

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		DescriptionItemEnum itemEnum = (DescriptionItemEnum)item;
		
		String value = enumMap.get(itemEnum.getS());
		// skip values without mapping
		if(value==null) {
			return;
		}
		
		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
		apusBuilder.addEnum(ctx.getActivePart(), targetType, value, true);
	}

}
