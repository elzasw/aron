package cz.aron.transfagent.elza.convertor;

import java.util.Map;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;

public class EdxApRefWithRole  implements EdxItemConvertor {


	private String partType;
	private String roleType;
	private String apRefType;

	public EdxApRefWithRole(final String partType,
			final String roleType, 
			final String apRefType) {
		this.partType = partType;
		this.roleType = roleType;
		this.apRefType = apRefType;
	}

	@Override
	public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
		if(item instanceof DescriptionItemUndefined) {
			return;
		}
		DescriptionItemAPRef apRef = (DescriptionItemAPRef)item;
		
		ElzaXmlReader elzaXmlReader = ctx.getElzaXmlReader();
		Map<String, AccessPoint> apMap = elzaXmlReader.getApMap();
		AccessPoint ap = apMap.get(apRef.getApid());
		if(ap==null) {
			throw new RuntimeException("Failed to convert AP: "+apRef.getApid() + ", ap not found");
		}
		
		ctx.addArchEntityRef(ap.getApe().getUuid());

		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
		
		Part part = apusBuilder.addPart(ctx.getActiveApu(), partType);
		// TODO: map spec to value
		apusBuilder.addEnum(part, roleType, apRef.getS(), true);		
		apusBuilder.addApuRef(part, apRefType, ap.getApe().getUuid());
		
	}

}
