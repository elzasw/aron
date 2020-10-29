package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainDao;
import org.springframework.stereotype.Service;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Service
public class MetadataTypeDao extends DomainDao<MetadataType, QMetadataType> {

    public MetadataTypeDao() {
        super(MetadataType.class, QMetadataType.class);
    }

    public MetadataType findForCode(String code) {
        return query().select(qObject).where(qObject.code.eq(code)).fetchOne();
    }
}
