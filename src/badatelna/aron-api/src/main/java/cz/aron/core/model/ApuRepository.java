package cz.aron.core.model;

import cz.inqool.eas.common.domain.DomainRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@Repository
public class ApuRepository extends DomainRepository<
        ApuEntity,
        ApuEntity,
        IndexedApu,
        ApuStore,
        ApuIndex> {
}