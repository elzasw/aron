package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainDao;
import org.springframework.stereotype.Service;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Service
public class ApuPartDao extends DomainDao<ApuPart, QApuPart> {

    public ApuPartDao() {
        super(ApuPart.class, QApuPart.class);
    }
}
