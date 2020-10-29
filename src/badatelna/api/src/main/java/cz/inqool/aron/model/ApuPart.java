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
public class ApuPart extends DomainObject {
    private String value;
    private int order;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ApuPartType type;

    @OneToMany(mappedBy = "parentPart", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<ApuPart> childParts = new ArrayList<>();

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ApuEntity apu;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    private ApuPart parentPart;

    @OneToMany(mappedBy = "apuPart", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<ApuPartItem> items = new ArrayList<>();
}
