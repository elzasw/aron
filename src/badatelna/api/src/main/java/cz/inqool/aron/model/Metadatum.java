package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
@Getter
@Setter
public class Metadatum extends DomainObject {
    private String value;

    @Enumerated(EnumType.STRING)
    private MetadataType type;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private DigitalObjectFile file;
}
