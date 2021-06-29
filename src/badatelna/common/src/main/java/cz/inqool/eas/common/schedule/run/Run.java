package cz.inqool.eas.common.schedule.run;

import cz.inqool.eas.common.dated.store.DatedObject;
import cz.inqool.eas.common.domain.DomainViews;
import cz.inqool.eas.common.schedule.job.Job;
import cz.inqool.entityviews.Viewable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.time.Instant;

/**
 * Běh časového úkolu.
 */
@Viewable
@DomainViews
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "eas_schedule_run")

public class Run extends DatedObject<Run> {
    @Fetch(FetchMode.SELECT)
    @ManyToOne
    protected Job job;

    protected String console;

    protected String result;

    @Enumerated(EnumType.STRING)
    protected RunState state;

    protected Instant startTime;
    protected Instant endTime;
}
