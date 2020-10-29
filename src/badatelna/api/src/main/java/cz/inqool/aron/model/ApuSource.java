package cz.inqool.aron.model;

import cz.inqool.aron.model.base.DomainObject;

import javax.persistence.Entity;
import java.time.LocalDateTime;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Entity
public class ApuSource extends DomainObject {
    private LocalDateTime published;
    private String data;
}
