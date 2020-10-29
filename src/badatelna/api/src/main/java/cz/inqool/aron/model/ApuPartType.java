package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Entity
@Getter
@Setter
public class ApuPartType extends DomainObject {
    private String code;
    private String label;
    private int order;
}
