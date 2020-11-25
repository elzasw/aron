package cz.aron.core.model;

import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;

/**
 * @author Lukas Jane (inQool) 10.11.2020.
 */
@Repository
public class ApuTreeViewStore extends DomainStore<ApuEntityTreeView, ApuEntityTreeView, QApuEntityTreeView> {
    public ApuTreeViewStore() {
        super(ApuEntityTreeView.class);
    }
}
