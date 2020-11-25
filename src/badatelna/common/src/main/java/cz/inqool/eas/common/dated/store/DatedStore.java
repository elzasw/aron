package cz.inqool.eas.common.dated.store;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.EntityPathBase;
import cz.inqool.eas.common.dated.Dated;
import cz.inqool.eas.common.domain.store.DomainStore;
import cz.inqool.eas.common.domain.store.list.QueryModifier;
import cz.inqool.eas.common.exception.PersistenceException;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * Database store for objects extending {@link DatedObject} with standard CRUD operations.
 *
 * Entities are not deleted from database, rather a deleted flag is set.
 *
 * @param <ROOT> Entity type
 * @param <PROJECTED> Entity projection type
 * @param <META_MODEL> Meta model for projection type
 */
public class DatedStore<
        ROOT extends Dated<ROOT>,
        PROJECTED extends Dated<ROOT>,
        META_MODEL extends EntityPathBase<PROJECTED>>
        extends DomainStore<
        ROOT,
        PROJECTED,
        META_MODEL
        > {
    protected final QDatedObject datedMetaModel;

    public DatedStore(Class<? extends PROJECTED> type) {
        super(type);

        this.datedMetaModel = new QDatedObject(this.metaModel.getMetadata());
    }

    /**
     * {@inheritDoc}
     *
     * Adds deleted check.
     */
    @Override
    public boolean exist(@Nonnull String id) {
        long count = query().
                select(metaModel).
                from(metaModel).
                where(datedMetaModel.id.eq(id)).
                where(datedMetaModel.deleted.isNull()).
                fetchCount();

        return count > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PROJECTED create(@Nonnull PROJECTED entity) {
        entity.setDeleted(null);
        return super.create(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends PROJECTED> create(@Nonnull Collection<? extends PROJECTED> entities) {
        entities.forEach(entity -> entity.setDeleted(null));
        return super.create(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PROJECTED update(@Nonnull PROJECTED entity) {
        if (isDeleted(entity.getId())) {
            throw new PersistenceException("Deleted entity can not be updated.");
        }
        entity.setUpdated(null); // to force hibernate to auto-generate new value
        entity.setDeleted(null);
        return super.update(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends PROJECTED> update(@Nonnull Collection<? extends PROJECTED> entities) {
        List<String> entityIDs = entities.stream()
                .map(Dated::getId)
                .collect(Collectors.toList());

        if (isAnyDeleted(entityIDs)) {
            throw new PersistenceException("Deleted entities can not be updated.");
        }
        entities.forEach(entity -> {
            entity.setUpdated(null); // to force hibernate to auto-generate new value
            entity.setDeleted(null);
        });
        return super.update(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PROJECTED delete(@Nonnull String id) {
        PROJECTED entity = findConnected(id);

        if (entity != null) {
            setDeletedFlag(entity);

            entityManager.flush();
            detachAll();
        }

        return entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<PROJECTED> delete(@Nonnull Collection<String> ids) {
        if (ids.isEmpty()) {
            return emptyList();
        }

        List<PROJECTED> deletedEntities = ids.stream()
                .filter(Objects::nonNull)
                .map(this::findConnected)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        setDeletedFlag(deletedEntities);

        entityManager.flush();
        detachAll();

        return deletedEntities;
    }

    /**
     * {@inheritDoc}
     *
     * Filter out deleted entities.
     */
    @Override
    public List<PROJECTED> listDefault(QueryModifier<ROOT, PROJECTED> queryModifier) {
        return super.list((query) -> {
            query.where(datedMetaModel.deleted.isNull());

            queryModifier.modify(query);
        });
    }

    /**
     * Tests if entity with specified id is among deleted.
     *
     * @param id Id of entity
     * @return deleted status
     */
    public boolean isDeleted(@Nonnull String id) {
        return query().
                select(metaModel).
                from(metaModel).
                where(datedMetaModel.id.eq(id)).
                where(datedMetaModel.deleted.isNotNull()).
                fetchCount() > 0;
    }

    /**
     * Tests if any entity from collection of ids is among deleted.
     *
     * @param ids Collection of ids
     * @return deleted status
     */
    public boolean isAnyDeleted(@Nonnull Collection<String> ids) {
        return query().
                select(metaModel).
                from(metaModel).
                where(datedMetaModel.id.in(ids)).
                where(datedMetaModel.deleted.isNotNull()).
                fetchCount() > 0;
    }

    /**
     * Restores deleted entity.
     *
     * @param id Id of deleted entity to restore
     *
     * @return restored entity
     */
    public PROJECTED restore(@Nonnull String id) {
        PROJECTED entity = findConnected(id);

        if (entity != null) {
            setRestoredFlag(entity);

            entityManager.flush();
            detachAll();
        }
        return entity;
    }

    /**
     * Sorts by created and id.
     */
    @Override
    protected OrderSpecifier<?>[] defaultListAllOrder() {
        return new OrderSpecifier[]{
                datedMetaModel.created.asc(),
                datedMetaModel.id.asc()
        };
    }

    /**
     * Updates entity with deleted flag.
     *
     * @param projected entity
     */
    protected void setDeletedFlag(PROJECTED projected) {
        projected.setDeleted(InstantGenerator.generateValue());
    }

    /**
     * Updates entity with deleted flag.
     *
     * @param projected entity
     */
    protected void setDeletedFlag(Collection<? extends PROJECTED> projected) {
        final Instant now = InstantGenerator.generateValue();

        projected.forEach(e -> e.setDeleted(now));
    }

    /**
     * Updates entity with restored flag.
     *
     * @param projected entity
     */
    protected void setRestoredFlag(PROJECTED projected) {
        projected.setDeleted(null);
    }
}
