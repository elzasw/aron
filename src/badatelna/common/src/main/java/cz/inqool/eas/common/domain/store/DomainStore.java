package cz.inqool.eas.common.domain.store;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import cz.inqool.eas.common.domain.Domain;
import cz.inqool.eas.common.domain.store.list.ListFunction;
import cz.inqool.eas.common.domain.store.list.QueryModifier;
import cz.inqool.eas.common.exception.GeneralException;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.inqool.eas.common.utils.AssertionUtils.gte;
import static cz.inqool.eas.common.utils.CollectionUtils.sortByIds;
import static java.util.Collections.emptyList;

/**
 * Database store for objects extending {@link DomainObject} with standard CRUD operations.
 *
 * @param <ROOT> Root of the projection type system
 * @param <PROJECTED> Projection type
 * @param <META_MODEL> Meta model for projection type
 */
public class DomainStore<ROOT extends Domain<ROOT>, PROJECTED extends Domain<ROOT>, META_MODEL extends EntityPathBase<PROJECTED>> {
    protected EntityManager entityManager;

    @Getter
    protected JPAQueryFactory queryFactory;

    @Getter
    private final Class<? extends PROJECTED> type;

    @Getter
    protected final META_MODEL metaModel;

    protected final QDomainObject domainMetaModel;

    public DomainStore(Class<? extends PROJECTED> type) {
        this.type = type;
        this.metaModel = deriveMetaModel(type);
        this.domainMetaModel = new QDomainObject(metaModel.getMetadata());
    }

    /**
     * Clones the store instance with different projection.
     *
     * Use {@link StoreCache#get(DomainStore, Class)} for caching the instances.
     *
     * @param <OTHER_PROJECTED> Another projected type
     *
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <OTHER_PROJECTED extends Domain<ROOT>> DomainStore<ROOT, OTHER_PROJECTED, ?> getProjection(Class<OTHER_PROJECTED> type) {
        try {
            for (Constructor<?> constructor : this.getClass().getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 1 && constructor.getParameterTypes()[0].equals(Class.class)) {
                    constructor = this.getClass().getDeclaredConstructor(Class.class);
                    DomainStore projectionStore = (DomainStore) constructor.newInstance(type);
                    projectionStore.setEntityManager(entityManager);
                    return projectionStore;
                }
                else if (constructor.getParameterCount() == 0) {
                    constructor = this.getClass().getDeclaredConstructor();
                    DomainStore projectionStore = (DomainStore) constructor.newInstance();
                    projectionStore.setEntityManager(entityManager);
                    return projectionStore;
                }
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new GeneralException("Can not find suitable Metamodel class.");
        } catch (NoSuchMethodException e) {
            throw new GeneralException("Can not create projection for store.");
        }
        throw new GeneralException("Can not create projection for store.");
    }

    /**
     * Creates new object in database.
     *
     * @param entity object to create
     * @return created object in detached state
     */
    public PROJECTED create(@NotNull PROJECTED entity) {
        entity = entityManager.merge(entity);
        entityManager.flush();
        detachAll();

        return entity;
    }

    /**
     * Creates collection of objects in a batch.
     *
     * @param entities collection of objects to create
     * @return collection of created objects
     */
    public Collection<? extends PROJECTED> create(@NotNull Collection<? extends PROJECTED> entities) {
        if (entities.isEmpty()) {
            return emptyList();
        }

        Set<? extends PROJECTED> saved = entities.stream()
                .map(entityManager::merge)
                .collect(Collectors.toSet());

        entityManager.flush();
        detachAll();

        return saved;
    }

    /**
     * Updates given object.
     *
     * @param entity object to update
     * @return saved object
     */
    public PROJECTED update(@NotNull PROJECTED entity) {
        PROJECTED obj = entityManager.merge(entity);

        entityManager.flush();
        detachAll();

        return obj;
    }

    /**
     * Updates given collection of objects in a batch.
     *
     * @param entities collection of objects to update
     * @return collection of updated objects
     */
    public Collection<? extends PROJECTED> update(@NotNull Collection<? extends PROJECTED> entities) {
        if (entities.isEmpty()) {
            return emptyList();
        }

        Set<? extends PROJECTED> saved = entities.stream()
                .map(entityManager::merge)
                .collect(Collectors.toSet());

        entityManager.flush();
        detachAll();

        return saved;
    }

    /**
     * Returns the number of stored objects.
     */
    public long countAll() {
        return query().
                select(metaModel).
                from(metaModel).
                fetchCount();
    }

    /**
     * Detaches all objects from JPA context.
     */
    public void detachAll() {
        entityManager.clear();
    }

    /**
     * Creates QueryDSL query object.
     */
    public JPAQuery<?> query() {
        return queryFactory.query();
    }

    /**
     * Creates QueryDSL delete query object.
     */
    public JPADeleteClause deleteQuery() {
        return queryFactory.delete(metaModel);
    }

    /**
     * Creates QueryDSL update query object.
     */
    public JPAUpdateClause updateQuery() {
        return queryFactory.update(metaModel);
    }

    /**
     * Returns object with given ID.
     *
     * @param id ID of object to return
     * @return found object or {@code null} if not found
     */
    public PROJECTED find(@NotNull String id) {
        PROJECTED entity = findConnected(id);

        if (entity != null) {
            detachAll();
        }

        return entity;
    }

    /**
     * Checks whether object with given ID exists in database.
     *
     * @param id ID of object
     * @return {@code true} if object exists, {@code false} otherwise
     */
    public boolean exist(@NotNull String id) {
        long count = query().
                select(domainMetaModel.id).
                from(metaModel).
                where(domainMetaModel.id.eq(id)).
                fetchCount();

        return count > 0;
    }

    /**
     * Returns objects using query modifier.
     *
     * @param modifier Query modifier
     *
     */
    public final List<PROJECTED> list(QueryModifier<ROOT, PROJECTED> modifier) {
        JPAQuery<PROJECTED> query = query().
                select(metaModel).
                from(metaModel);

        modifier.modify(query);

        // need to define ordering to not retrieve records in random order
        // (and same records returned multiple times in separate calls).
        query.orderBy(defaultListAllOrder());

        List<PROJECTED> list = query.fetch();

        detachAll();

        return list;
    }

    /**
     * Returns objects using query modifier.
     *
     * Difference from {@link DomainStore#list(QueryModifier)} is that
     * it can be overridden in subclasses to add default modifier to all list-like methods.
     *
     * @param modifier Query modifier
     *
     */
    public List<PROJECTED> listDefault(QueryModifier<ROOT, PROJECTED> modifier) {
        return list(modifier);
    }

    /**
     * Finds the objects corresponding to the specified list of IDs.
     *
     * @param ids list of IDs
     * @return collection of found instances
     */
    public List<PROJECTED> listByIds(@NotNull List<String> ids, ListFunction<ROOT, PROJECTED> listFunction) {
        if (ids.isEmpty()) {
            return emptyList();
        }

        List<PROJECTED> list = listFunction.call((query) -> query.where(domainMetaModel.id.in(ids)));

        return sortByIds(ids, list, Domain::getId);
    }

    /**
     * Returns objects in batches.
     *
     * @param offset defines the offset for the query results
     * @param limit  defines the limit / max results for the query results
     * @return collection of found instances
     */
    public List<PROJECTED> listByWindow(long offset, long limit, ListFunction<ROOT, PROJECTED> listFunction) {
        gte(offset, 0L, () -> new IllegalArgumentException("Offset must be a non-negative number."));
        gte(limit, 0L, () -> new IllegalArgumentException("Limit must be a non-negative number."));

        return listFunction.call((query) -> {
            if (offset != 0) {
                query.offset(offset);
            }

            if (limit != 0) {
                query.limit(limit);
            }
        });
    }

    /**
     * Returns all instances.
     *
     * @param listFunction List function to use.
     *
     */
    public List<PROJECTED> listAll(ListFunction<ROOT, PROJECTED> listFunction) {
        return listFunction.call((query) -> {});
    }

    /**
     * Finds the objects corresponding to the specified list of IDs using {@link DomainStore#listDefault(QueryModifier)}.
     *
     * @param ids list of IDs
     * @return collection of found objects
     */
    public List<PROJECTED> listByIds(@NotNull List<String> ids) {
        return listByIds(ids, this::listDefault);
    }

    /**
     * Returns objects in batches using {@link DomainStore#listDefault(QueryModifier)}.
     *
     * @param offset defines the offset for the query results
     * @param limit  defines the limit / max results for the query results
     * @return collection of found objects
     */
    public List<PROJECTED> listByWindow(long offset, long limit) {
        return listByWindow(offset, limit, this::listDefault);
    }

    /**
     * Returns all objects using {@link DomainStore#listDefault(QueryModifier)}.
     *
     * @return collection of found objects
     */
    public List<PROJECTED> listAll() {
        return listAll(this::listDefault);
    }

    /**
     * Deletes object with given id.
     *
     * @param id id of instance to delete
     * @return resultant object or {@code null} if the object was not found
     */
    public PROJECTED delete(@NotNull String id) {
        PROJECTED entity = findConnected(id);

        if (entity != null) {
            entityManager.remove(entity);
            entityManager.flush();
        }
        return entity;
    }

    /**
     * Deletes given collection of objects in a batch.
     *
     * @param ids collection of ids of objects to delete
     * @return collection of deleted objects
     */
    public Collection<PROJECTED> delete(@NotNull Collection<String> ids) {
        if (ids.isEmpty()) {
            return emptyList();
        }

        List<PROJECTED> deletedEntities = ids.stream()
                .filter(Objects::nonNull)
                .map(this::findConnected)
                .filter(Objects::nonNull)
                .peek(entityManager::remove)
                .collect(Collectors.toList());

        entityManager.flush();

        return deletedEntities;
    }

    /**
     * Returns dummy object with specified id.
     */
    @SneakyThrows
    public PROJECTED getRef(String id) {
        Constructor<? extends PROJECTED> constructor = type.getDeclaredConstructor();
        PROJECTED instance = constructor.newInstance();
        instance.setId(id);

        return instance;
    }

    /**
     * Returns the single object with given ID still connected to JPA context.
     *
     * For internal use only.
     *
     * @param id ID of instance to be returned
     * @return found instance or {@code null} if not found
     */
    protected PROJECTED findConnected(@NotNull String id) {
        JPAQuery<PROJECTED> query = query().
                select(metaModel).
                from(metaModel)
                .where(domainMetaModel.id.eq(id));

        return query.fetchFirst();
    }

    /**
     * Sorts by id.
     */
    protected OrderSpecifier<?>[] defaultListAllOrder() {
        return new OrderSpecifier[]{domainMetaModel.id.asc()};
    }

    @SuppressWarnings("unchecked")
    private META_MODEL deriveMetaModel(Class<? extends PROJECTED> type) {
        String simpleName = type.getSimpleName();
        String qTypeName = type.getPackageName() + ".Q" + simpleName;

        try {
            Class<META_MODEL> qType = (Class<META_MODEL>) Class.forName(qTypeName);
            Constructor<META_MODEL> constructor = qType.getConstructor(String.class);
            return constructor.newInstance(simpleName);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException | ClassNotFoundException e) {
            throw new GeneralException("Error creating Q object for " + simpleName);
        }
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.queryFactory = new JPAQueryFactory(entityManager);
    }
}
