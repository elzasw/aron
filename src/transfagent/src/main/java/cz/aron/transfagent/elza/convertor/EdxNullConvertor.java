package cz.aron.transfagent.elza.convertor;

import cz.tacr.elza.schema.v2.DescriptionItem;

/**
 * Convertor without ouput 
 * 
 *
 */
public class EdxNullConvertor implements EdxItemConvertor {

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		// nop		
	}

}
