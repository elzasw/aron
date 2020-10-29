package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
@Getter
@Setter
public class ApuAttachment extends DomainObject {
    private String name;

    @OneToOne(mappedBy = "attachment")
    @Fetch(FetchMode.SELECT)
    private DigitalObjectFile file;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ApuEntity apu;
}
