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
}
