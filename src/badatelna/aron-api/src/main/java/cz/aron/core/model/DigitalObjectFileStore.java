package cz.aron.core.model;

import cz.inqool.eas.common.domain.store.DomainStore;
import org.springframework.stereotype.Repository;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Repository
public class DigitalObjectFileStore extends DomainStore<DigitalObjectFile, DigitalObjectFile, QDigitalObjectFile> {

    public DigitalObjectFileStore() {
        super(DigitalObjectFile.class);
    }
}
