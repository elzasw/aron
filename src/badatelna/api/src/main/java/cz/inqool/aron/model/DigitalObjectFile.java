package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainObject;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
@Getter
@Setter
public class DigitalObjectFile extends DomainObject {
    private String permalink;
    private int order;

    @Enumerated(EnumType.STRING)
    private DigitalObjectType type;

    @OneToMany(mappedBy = "file", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<Metadatum> metadata = new ArrayList<>();

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ApuAttachment attachment;
}
