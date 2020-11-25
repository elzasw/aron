package cz.aron.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.domain.store.DomainObject;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import java.time.LocalDateTime;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Getter
@Setter
@Entity
public class ApuSource extends DomainObject<ApuSource> {
    private LocalDateTime published;

    @JsonIgnore
    private String data;
}
