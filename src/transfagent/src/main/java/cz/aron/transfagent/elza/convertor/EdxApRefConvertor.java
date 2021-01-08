package cz.aron.transfagent.elza.convertor;

import java.util.Map;
import java.util.UUID;

import org.springframework.util.CollectionUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;

public class EdxApRefConvertor implements EdxItemConvertor {

	private final String targetType;
	
	private final ContextDataProvider dataProvider;

	public EdxApRefConvertor(final String targetType, ContextDataProvider dataProvider) {
		this.targetType = targetType;
		this.dataProvider = dataProvider;
	}

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		DescriptionItemAPRef apRef = (DescriptionItemAPRef)item;
		
		ElzaXmlReader elzaXmlReader = ctx.getElzaXmlReader();
		Map<String, AccessPoint> apMap = elzaXmlReader.getApMap();
		AccessPoint ap = apMap.get(apRef.getApid());
		if(ap==null) {
			throw new RuntimeException("Failed to convert AP: "+apRef.getApid() + ", ap not found");
		}
		
		UUID apUuid = UUID.fromString(ap.getApe().getUuid());
		
		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
		
		var uuids = dataProvider.findByUUIDWithParents(apUuid);
		if (CollectionUtils.isEmpty(uuids)) {
		    ctx.addArchEntityRef(apUuid);
		    apusBuilder.addApuRef(ctx.getActivePart(), targetType, apUuid);
		} else {
		    uuids.forEach(uuid -> ctx.addArchEntityRef(uuid));
		    apusBuilder.addApuRefsFirstVisible(ctx.getActivePart(), targetType, uuids);
		}

	}

}
