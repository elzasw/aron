package cz.aron.core.relation;

import cz.inqool.eas.common.domain.store.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

/**
 * @author Lukas Jane (inQool) 11.12.2020.
 */
@Getter
@Setter
@Entity
public class Relation extends DomainObject<Relation> {
    private String source;
    private String relation;
    private String target;
}
