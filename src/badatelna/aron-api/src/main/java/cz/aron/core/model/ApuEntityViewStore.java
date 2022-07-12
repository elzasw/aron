package cz.aron.core.model;

import org.springframework.stereotype.Repository;

import cz.inqool.eas.common.domain.store.DomainStore;

@Repository
public class ApuEntityViewStore extends DomainStore<ApuEntityView, ApuEntityView, QApuEntityView> {
    public ApuEntityViewStore() {
        super(ApuEntityView.class);
    }
}
