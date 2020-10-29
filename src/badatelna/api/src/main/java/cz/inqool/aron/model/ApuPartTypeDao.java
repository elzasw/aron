package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainDao;
import org.springframework.stereotype.Service;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Service
public class ApuPartTypeDao extends DomainDao<ApuPartType, QApuPartType> {

    public ApuPartTypeDao() {
        super(ApuPartType.class, QApuPartType.class);
    }

    public ApuPartType findForCode(String code) {
        return query().select(qObject).where(qObject.code.eq(code)).fetchOne();
    }
}
