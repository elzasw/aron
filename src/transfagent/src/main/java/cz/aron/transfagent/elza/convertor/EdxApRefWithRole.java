package cz.aron.transfagent.elza.convertor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.CollectionUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.apux._2020.Part;
import cz.aron.transfagent.config.ConfigElzaArchDescApMapping;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.transformation.ArchEntityInfo;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;

public class EdxApRefWithRole implements EdxItemConvertor {

    private final String partType;
    private final ContextDataProvider dataProvider;
    private final Map<String, String> specMap;    
    private final List<ConfigElzaArchDescApMapping> apMappings;

    public EdxApRefWithRole(String partType,
            final ContextDataProvider dataProvider, 
            final Map<String, String> specMap,
            final List<ConfigElzaArchDescApMapping> apMappings) {
        this.partType = partType;
        this.dataProvider = dataProvider;
        this.specMap = specMap;
        if (apMappings==null) {
        	this.apMappings = Collections.emptyList();
        } else {
        	this.apMappings = apMappings;
        }
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
        if(ap == null) {
            throw new RuntimeException("Failed to convert AP: " + apRef.getApid() + ", ap not found");
        }

        ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
        Part part = ApuSourceBuilder.getFirstPart(ctx.getActiveApu(), partType);
        if(part == null) {
            part = ApuSourceBuilder.addPart(ctx.getActiveApu(), partType);
        }

        UUID apUuid = UUID.fromString(ap.getApe().getUuid());
        var archEntityInfo = dataProvider.getArchivalEntityWithParentsByUuid(apUuid);

        String t = this.specMap.get(apRef.getS());
        if(t == null) {
            throw new RuntimeException("Missing mapping for type: " + apRef.getT() + ", spec: " + apRef.getS() + ", ap not found");
        }
        
        for(var apMapping:apMappings) {
        	if (Objects.equals(apMapping.getSpec(), apRef.getS())&&Objects.equals(apMapping.getUuid(), ap.getApe().getUuid()) ) {
        		ApuSourceBuilder.addEnum(part, apMapping.getCode(), apMapping.getName(), true);
        	}
        }

        //apusBuilder.addEnum(part, roleType, s, true);
        if (CollectionUtils.isEmpty(archEntityInfo)) {
            ArchEntityInfo aei = new ArchEntityInfo(apUuid, ap.getApe().getT()); 
            ctx.addArchEntityRef(aei);
            apusBuilder.addApuRef(part, t, apUuid);
        } else {
            List<UUID> uuids = new ArrayList<>(archEntityInfo.size());
            for(ArchEntityInfo aei : archEntityInfo) {
                var uuid = aei.getUuid();
                uuids.add(uuid);
                ctx.addArchEntityRef(aei);
            }
            apusBuilder.addApuRefsFirstVisible(part, t, uuids);
        }
    }

}
