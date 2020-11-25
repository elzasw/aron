package cz.aron.core.model;

import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Service;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Service
public class ApuSourceStore extends DomainStore<ApuSource, ApuSource, QApuSource> {

    public ApuSourceStore() {
        super(ApuSource.class);
    }
}
