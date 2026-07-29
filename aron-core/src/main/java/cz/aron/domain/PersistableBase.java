package cz.aron.domain;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

/**
 * Base class for entities that assign their own primary key (via {@code IdService}) instead of
 * relying on a generated identity. Implements {@link Persistable} with a transient "new" flag so
 * that Spring Data JPA persists (not merges) an entity that already carries an assigned id.
 *
 * <p>The {@code id} column is named {@code id} here; each subclass remaps it to its own column with
 * {@code @AttributeOverride(name = "id", column = @Column(name = "..."))}.
 */
@MappedSuperclass
public abstract class PersistableBase implements Persistable<Long> {

    @Id
    @Column(name = "id")
    private long id;

    // true until the row is loaded from or written to the DB; drives isNew() so that Spring Data
    // persists (not merges) entities that already carry an application-assigned id
    @Transient
    private transient boolean isNew = true;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

}
