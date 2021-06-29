package cz.aron.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.domain.store.DomainObject;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lukas Jane (inQool) 10.11.2020.
 */
@Getter
@Immutable
@Entity(name = "apu_entity_tree_view")
@Table(name = "apu_entity")
@BatchSize(size = 100)
public class ApuEntityTreeView extends DomainObject<ApuEntityTreeView> {
    private String name;
    private String description;
    private int order;

    @Enumerated(EnumType.STRING)
    private ApuType type;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private ApuEntityTreeView parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    @OrderBy("order")
    private List<ApuEntityTreeView> children = new ArrayList<>();
}
