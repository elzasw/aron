package cz.inqool.eas.common.history;

import cz.inqool.eas.common.authored.store.AuthoredObject;
import cz.inqool.eas.common.domain.DomainViews;
import cz.inqool.eas.common.history.operation.HistoryOperationReference;
import cz.inqool.entityviews.Viewable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;

/**
 * Záznam v zjednodušeném logu změn na objektech.
 */
@Viewable
@DomainViews
@Getter
@Setter
@BatchSize(size = 100)
@Entity
@Table(name = "eas_history")
public class History extends AuthoredObject<History> {
    /**
     * ID entity, ke které se záznam váže.
     *
     * Systém spoléhá na to, že ID entity je unikátní napříč systémem.
     */
    protected String entityId;

    /**
     * Operace změny.
     *
     * Default se vybírají hodnoty z HistoryOperation. Aplikace může doplnit svoje.
     */
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "operation_id"))
    @AttributeOverride(name = "name", column = @Column(name = "operation_name"))
    protected HistoryOperationReference operation;

    /**
     * Popis změny.
     */
    protected String description;
}
