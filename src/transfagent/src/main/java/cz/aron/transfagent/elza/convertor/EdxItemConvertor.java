package cz.aron.transfagent.elza.convertor;

import cz.tacr.elza.schema.v2.DescriptionItem;

public interface EdxItemConvertor {
	void convert(EdxItemCovertContext ctx, DescriptionItem item);
}
