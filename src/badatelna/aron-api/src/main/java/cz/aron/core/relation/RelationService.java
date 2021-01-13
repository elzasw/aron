package cz.aron.core.relation;

import cz.inqool.eas.common.domain.DomainService;
import org.springframework.stereotype.Service;

/**
 * @author Lukas Jane (inQool) 03.11.2020.
 */
@Service
public class RelationService extends DomainService<
        Relation,
        Relation,
        Relation,
        Relation,
        Relation,
        RelationRepository
        > {
}