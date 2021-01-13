package cz.aron.core.integration;

import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Lukas Jane (inQool) 17.12.2020.
 */
@Repository
public class QueuedApuStore extends DomainStore<QueuedApu, QueuedApu, QQueuedApu> {

    public QueuedApuStore() {
        super(QueuedApu.class);
    }

    public List<QueuedApu> getBatchToResolve() {
        List<QueuedApu> results = query().from(metaModel).select(metaModel).where(metaModel.requestSent.isFalse()).fetch();
        detachAll();
        return results;
    }

    public void removeForApuId(String apuId) {
        queryFactory.delete(metaModel).where(metaModel.apuId.eq(apuId)).execute();
    }
}
