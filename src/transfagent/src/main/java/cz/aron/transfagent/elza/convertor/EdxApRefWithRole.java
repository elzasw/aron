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
	private final String roleType;
	private final String apRefType;
	
	private final ContextDataProvider dataProvider;
    private Map<String, String> specMap;

    public EdxApRefWithRole(String partType,
            String roleType,
            String apRefType, 
            final ContextDataProvider dataProvider, 
            final Map<String, String> specMap) {
        this.partType = partType;
        this.roleType = roleType;
        this.apRefType = apRefType;
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
		
		UUID apUuid = UUID.fromString(ap.getApe().getUuid());		
		ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
		Part part = apusBuilder.addPart(ctx.getActiveApu(), partType);
		String s;
		if(specMap!=null) {
		    s = this.specMap.get(apRef.getS());
		    if(s==null) {
		        throw new RuntimeException("Missing mapping for specification: "+apRef.getS());
		    }
		} else {
		    s = apRef.getS();
		}
		
		apusBuilder.addEnum(part, roleType, s, true);
		var uuids = dataProvider.findByUUIDWithParents(apUuid);
        if (CollectionUtils.isEmpty(uuids)) {
            ctx.addArchEntityRef(apUuid);
            apusBuilder.addApuRef(part, apRefType, apUuid);
        } else {
            uuids.forEach(uuid -> ctx.addArchEntityRef(uuid));
            apusBuilder.addApuRefsFirstVisible(part, apRefType, uuids);
        }		
	}

}
