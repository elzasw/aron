package cz.aron.transfagent.elza.convertor;

import java.util.Map;
import java.util.UUID;

import org.springframework.util.CollectionUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;

public class EdxApRefWithRole  implements EdxItemConvertor {

	private final  String partType;
	
	private final ContextDataProvider dataProvider;
    private Map<String, String> specMap;

    public EdxApRefWithRole(String partType,
            final ContextDataProvider dataProvider, 
            final Map<String, String> specMap) {
        this.partType = partType;
        this.dataProvider = dataProvider;
        this.specMap = specMap;
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
		
        ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
        Part part = ApuSourceBuilder.addPart(ctx.getActiveApu(), partType);

        UUID apUuid = UUID.fromString(ap.getApe().getUuid());
		var uuids = dataProvider.findByUUIDWithParents(apUuid);
		
		String t = this.specMap.get(apRef.getS());
		if(t==null) {
		    throw new RuntimeException("Missing mapping for type: "+apRef.getT()+", spec: "+apRef.getS() + ", ap not found");
		}
		
		//apusBuilder.addEnum(part, roleType, s, true);		
        if (CollectionUtils.isEmpty(uuids)) {
            ctx.addArchEntityRef(apUuid);
            apusBuilder.addApuRef(part, t, apUuid);
        } else {
            uuids.forEach(uuid -> ctx.addArchEntityRef(uuid));
            apusBuilder.addApuRefsFirstVisible(part, t, uuids);
        }		
	}

}
