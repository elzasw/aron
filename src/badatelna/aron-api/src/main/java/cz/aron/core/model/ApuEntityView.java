package cz.aron.core.model;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import cz.inqool.eas.common.domain.store.DomainObject;
import lombok.Getter;

@Getter
@Immutable
@Entity(name = "apu_entity_view")
@Table(name = "apu_entity")
@BatchSize(size = 100)
public class ApuEntityView extends DomainObject<ApuEntityView> {

    private String name;
    private String description;
    private int order;

    

}
