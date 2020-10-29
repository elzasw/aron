package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
@Getter
@Setter
public class MetadataType extends DomainObject {
    private String code;
    private String name;
    private int order;
}
