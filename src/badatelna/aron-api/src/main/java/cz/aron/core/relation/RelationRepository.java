package cz.aron.core.relation;

import cz.inqool.eas.common.domain.DomainRepository;
import cz.inqool.eas.common.domain.index.DomainIndex;
import org.springframework.stereotype.Repository;

/**
 * @author Lukas Jane (inQool) 11.12.2020.
 */
@Repository
public class RelationRepository extends DomainRepository<
        Relation,
        Relation,
        IndexedRelation,
        RelationStore,
        DomainIndex<Relation, Relation, IndexedRelation>> {
}
