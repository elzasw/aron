package cz.aron.transfagent.elza.convertor;

import java.util.UUID;

import cz.aron.apux.ApuSourceBuilder;
import cz.tacr.elza.schema.v2.DescriptionItem;
import cz.tacr.elza.schema.v2.DescriptionItemUndefined;
import cz.tacr.elza.schema.v2.DescriptionItemUriRef;

public class EdxLinkConvertor implements EdxItemConvertor {
    
    final private String targetTypeRef;
    final private String targetTypeLink;

    public EdxLinkConvertor(final String targetTypeRef,
                            final String targetTypeLink) {
        this.targetTypeRef = targetTypeRef;
        this.targetTypeLink = targetTypeLink;
    }

    @Override
    public void convert(EdxItemCovertContext ctx, DescriptionItem item) {
        if(item instanceof DescriptionItemUndefined) {
            return;
        }
        ApuSourceBuilder apusBuilder = ctx.getApusBuilder();
        
        DescriptionItemUriRef uriRef = (DescriptionItemUriRef)item;
        if("elza-node".equals(uriRef.getSchm())) {
            // link to another apu
            String uuid = uriRef.getUri().substring(9);
            if(uuid.startsWith("//")) {
                uuid = uuid.substring(2);
            }
            apusBuilder.addApuRef(ctx.getActivePart(), targetTypeRef, UUID.fromString(uuid));
        } else {
            // prepare external link
            apusBuilder.addLink(ctx.getActivePart(), targetTypeLink, uriRef.getUri(), uriRef.getLbl());
        }
    }

}
