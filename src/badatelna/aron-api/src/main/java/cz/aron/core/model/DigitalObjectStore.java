package cz.aron.core.model;

import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Service;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Service
public class DigitalObjectStore extends DomainStore<DigitalObject, DigitalObject, QDigitalObject> {

    public DigitalObjectStore() {
        super(DigitalObject.class);
    }
}
