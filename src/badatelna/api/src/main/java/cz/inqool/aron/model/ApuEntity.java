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
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Entity
@Getter
@Setter
public class ApuEntity extends DomainObject {
    private String name;
    private String permalink;
    private boolean published;

    private String source;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ApuEntity parent;

    @OneToMany(mappedBy = "apu", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<ApuPart> parts = new ArrayList<>();


    @OneToMany(mappedBy = "apu", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<ApuAttachment> attachments;

    @Enumerated(EnumType.STRING)
    private ApuType type;
}
