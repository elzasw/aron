package cz.aron.transfagent.elza.convertor;

import java.util.Map;
import java.util.UUID;

import org.springframework.util.CollectionUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.repository.ArchivalEntityRepository;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;

public class EdxApRefConvertor implements EdxItemConvertor {

	private final String targetType;
	
	private final ArchivalEntityRepository archivalEntityRepository;

	public EdxApRefConvertor(final String targetType, ArchivalEntityRepository archivalEntityRepository) {
		this.targetType = targetType;
		this.archivalEntityRepository = archivalEntityRepository;
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
		
		var uuids = archivalEntityRepository.findByUUIDWithParents(apUuid);
		if (CollectionUtils.isEmpty(uuids)) {
		    ctx.addArchEntityRef(apUuid);
		    apusBuilder.addApuRef(ctx.getActivePart(), targetType, apUuid);
		} else {
		    ctx.addArchEntityRef(apUuid);
		    apusBuilder.addApuRefsFirstVisible(ctx.getActivePart(), targetType, uuids);
		}

	}

}
