package cz.aron.core.model;

import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Repository
public class DigitalObjectStore extends DomainStore<DigitalObject, DigitalObject, QDigitalObject> {

    public DigitalObjectStore() {
        super(DigitalObject.class);
    }

    public long disconnectDaosByApuSourceId(String id) {
        long numModified = entityManager.createQuery("UPDATE DigitalObject do SET do.apu=NULL WHERE do IN (SELECT do FROM DigitalObject do INNER JOIN ApuEntity ae ON do.apu=ae WHERE ae.source.id=?1)")
        .setParameter(1,id).executeUpdate();
        entityManager.flush();
        return numModified;
    }

}
