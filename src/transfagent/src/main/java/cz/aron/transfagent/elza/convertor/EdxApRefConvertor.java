package cz.aron.transfagent.elza.convertor;

import java.util.Map;
import java.util.UUID;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;

public class EdxApRefConvertor implements EdxItemConvertor {

	final private String targetType;

	public EdxApRefConvertor(final String targetType) {
		this.targetType = targetType;
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
		
		ctx.addArchEntityRef(apUuid);

		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
		apusBuilder.addApuRef(ctx.getActivePart(), targetType, apUuid);
		
	}

}
