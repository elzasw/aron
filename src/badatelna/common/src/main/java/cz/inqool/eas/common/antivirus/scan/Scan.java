package cz.inqool.eas.common.antivirus.scan;

import cz.inqool.eas.common.dated.store.DatedObject;
import cz.inqool.eas.common.domain.DomainViews;
import cz.inqool.eas.common.storage.file.File;
import cz.inqool.entityviews.Viewable;
import cz.inqool.entityviews.ViewableMapping;
import cz.inqool.entityviews.ViewableProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import static cz.inqool.eas.common.domain.DomainViews.*;


/**
 * Scanovaní souboru antivirem.
 */
@Viewable
@DomainViews
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "eas_scan")
public class Scan extends DatedObject<Scan> {

    @ViewableProperty(views = {CREATE, UPDATE, DETAIL})
    @ViewableMapping(views = {CREATE, UPDATE}, useRef = true)
    @NotNull
    @Fetch(FetchMode.SELECT)
    @ManyToOne
    protected File content;

    @Enumerated(EnumType.STRING)
    protected ScanResult result;
}
