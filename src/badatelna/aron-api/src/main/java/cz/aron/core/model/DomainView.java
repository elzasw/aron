package cz.aron.core.model;

import lombok.Getter;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Lukas Jane (inQool) 10.11.2020.
 */

@MappedSuperclass
public class DomainView {
    /**
     * @see cz.inqool.eas.common.domain.store.DomainObject#id
     */
    @Id
    @Getter
    protected String id;
}
