package cz.aron.core.integration;

import cz.inqool.eas.common.dated.store.InstantGenerator;
import cz.inqool.eas.common.domain.store.DomainObject;
import cz.inqool.entityviews.ViewableProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.time.Instant;

import static cz.inqool.eas.common.domain.DomainViews.DEFAULT;
import static cz.inqool.eas.common.domain.DomainViews.IDENTIFIED;

/**
 * @author Lukas Jane (inQool) 27.10.2020.
 */
@Getter
@Setter
@Entity
public class QueuedApu extends DomainObject<QueuedApu> {
    @ViewableProperty(views = {DEFAULT, IDENTIFIED})
    @Column(updatable = false, nullable = false)
    @GeneratorType(type = InstantGenerator.class, when = GenerationTime.INSERT)
    protected Instant created;

    private String apuId;
    private String sourceApuId;

    private boolean requestSent;
}
