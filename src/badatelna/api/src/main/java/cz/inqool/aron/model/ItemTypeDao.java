package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainDao;
import org.springframework.stereotype.Service;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Service
public class ItemTypeDao extends DomainDao<ItemType, QItemType> {

    public ItemTypeDao() {
        super(ItemType.class, QItemType.class);
    }

    public ItemType findForCode(String code) {
        return query().select(qObject).where(qObject.code.eq(code)).fetchOne();
    }
}
