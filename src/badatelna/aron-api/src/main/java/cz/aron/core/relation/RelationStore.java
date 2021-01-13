package cz.aron.core.relation;

import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 11.12.2020.
 */
@Repository
public class RelationStore extends DomainStore<Relation, Relation, QRelation> {

    public RelationStore() {
        super(Relation.class);
    }

    public List<String> findExistingIds(String sourceId) {
        return query().select(metaModel.id).from(metaModel).where(metaModel.source.eq(sourceId)).fetch();
    }

    public List<String> listIdsTargetingIds(Collection<String> targetIds) {
        return query().select(metaModel.id).from(metaModel).where(metaModel.target.in(targetIds)).fetch();
    }
}
