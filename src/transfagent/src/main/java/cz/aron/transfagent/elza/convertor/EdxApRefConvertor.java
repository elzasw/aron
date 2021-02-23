package cz.aron.transfagent.elza.convertor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.util.CollectionUtils;

import cz.aron.apux.ApuSourceBuilder;
import cz.aron.transfagent.elza.ElzaXmlReader;
import cz.aron.transfagent.transformation.ArchEntityInfo;
import cz.aron.transfagent.transformation.ContextDataProvider;
import cz.tacr.elza.schema.v2.AccessPoint;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemAPRef;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;

public class EdxApRefConvertor implements EdxItemConvertor {

    private final String targetType;

    private final ContextDataProvider dataProvider;

    public EdxApRefConvertor(final String targetType, ContextDataProvider dataProvider) {
        this.targetType = targetType;
        this.dataProvider = dataProvider;
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

        var archEntityInfo = dataProvider.getArchivalEntityWithParentsByUuid(apUuid);
        if (CollectionUtils.isEmpty(archEntityInfo)) {
            ctx.addArchEntityRef(new ArchEntityInfo(apUuid, ap.getApe().getT()) );
            apusBuilder.addApuRef(ctx.getActivePart(), targetType, apUuid);
        } else {
            List<UUID> uuids = new ArrayList<>(archEntityInfo.size());
            for(var aei: archEntityInfo) {
                uuids.add(aei.getUuid());
                ctx.addArchEntityRef(aei);
            }
            apusBuilder.addApuRefsFirstVisible(ctx.getActivePart(), targetType, uuids);
        }
    }

}
