package cz.aron.core.model;

import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Repository
public class ApuStore extends DomainStore<ApuEntity, ApuEntity, QApuEntity> {

    public ApuStore() {
        super(ApuEntity.class);
    }

    public List<String> findBySourceId(String id) {
        return query().select(metaModel.id).from(metaModel).where(metaModel.source.id.eq(id)).fetch();
    }
}
