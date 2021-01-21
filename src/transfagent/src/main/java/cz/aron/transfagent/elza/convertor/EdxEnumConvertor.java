package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemEnum;
import cz.tacr.elza.schema.v2.DescriptionItemInteger;

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
	    
	    String specCode = null;
	    if(item instanceof DescriptionItemEnum) {
	        DescriptionItemEnum itemEnum = (DescriptionItemEnum)item;
	        specCode = itemEnum.getS();
	    } else
	    if(item instanceof DescriptionItemInteger) {
	        DescriptionItemInteger dii = (DescriptionItemInteger)item;
	        specCode = dii.getS();
	    }
		
		String value = enumMap.get(specCode);
		// skip values without mapping
		if(value==null) {
			return;
		}
		
		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
		apusBuilder.addEnum(ctx.getActivePart(), targetType, value, true);
	}

}
