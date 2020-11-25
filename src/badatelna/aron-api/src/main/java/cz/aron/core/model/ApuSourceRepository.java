package cz.aron.core.model;

import cz.inqool.eas.common.domain.DomainRepository;
import cz.inqool.eas.common.domain.index.DomainIndex;
import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@Repository
public class ApuSourceRepository extends DomainRepository<
        ApuSource,
        ApuSource,
        IndexedApuSource,
        DomainStore<ApuSource, ApuSource, QApuSource>,
        DomainIndex<ApuSource, ApuSource, IndexedApuSource>> {
}