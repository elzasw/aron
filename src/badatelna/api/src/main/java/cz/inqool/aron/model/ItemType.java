package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
@Getter
@Setter
public class ItemType extends DomainObject {
    private String code;
    private int order;

    @Enumerated(EnumType.STRING)
    private DataType dataType;
}
