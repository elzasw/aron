package cz.aron.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.domain.store.DomainObject;
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
public class ApuPart extends DomainObject<ApuPart> {
    private String value;
    private String type;

    @OneToMany(mappedBy = "parentPart", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<ApuPart> childParts = new ArrayList<>();

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private ApuEntity apu;
    @ManyToOne
    @Fetch(FetchMode.SELECT)
    @JsonIgnore
    private ApuPart parentPart;

    @OneToMany(mappedBy = "apuPart", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 100)
    private List<ApuPartItem> items = new ArrayList<>();

    public ApuEntity findRootApuEntity() {
        if (apu != null) {
            return apu;
        }
        if (parentPart != null) {
            return parentPart.findRootApuEntity();
        }
        return null;
    }
}
