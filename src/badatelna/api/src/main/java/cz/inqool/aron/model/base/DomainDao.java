package cz.inqool.aron.model.base;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.Getter;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

/**
 * Facade around JPA {@link EntityManager} and QueryDSL providing CRUD operations.
 *
 * <p>
 *     All the entity instances should have externally set {@link DomainObject#id} to an {@link java.util.UUID},
 *     therefore we do not now if the instance is already saved in database or is completely new. Because of that, there
 *     is no create/update method, only the {@link DomainDao#save(DomainObject)}, which handles both cases.
 * </p>
 *
 * <p>
 *     JPA concept of managed/detached instances is prone to development errors. Therefore every instance should be
 *     detached upon retrieving. All methods in {@link DomainDao} adhere to this rule.
 * </p>
 *
 * <p>
 *     After every saving of instance, the {@link EntityManager}'s context is flushed. This is a rather expensive
 *     operation and therefore if more than a few instances should be saved in a row, one should use
 *     {@link DomainDao#save(Collection)} which provides batching and only after saving all instances the context is
 *     flushed.
 * </p>
 *
 * @param <T> Type of entity to hold
 * @param <Q> Type of query object
 */
public abstract class DomainDao<T extends DomainObject, Q extends EntityPathBase<T>> {
    /**
     * Entity manager used for JPA
     */
    protected EntityManager entityManager;

    /**
     * QueryDSL query factory
     */
    protected JPAQueryFactory queryFactory;

    /**
     * Entity class object
     */
    @Getter
    protected Class<T> type;

    /**
     * QueryDSL meta class object
     */
    protected Class<Q> qType;

    protected final Q qObject;

    public DomainDao(Class<T> type, Class<Q> qType) {
        this.type = type;
        this.qType = qType;

        this.qObject = constructQObject(type, qType);
    }

    /**
     * Finds all instances.
     *
     * <p>
     *     Possibly very cost operation. Should be used only if we know there is not many instances or for
     *     debugging purposes.
     * </p>
     * @return {@link Collection} of instances
     */
    public Collection<T> findAll() {
        JPAQuery<T> query = query().select(qObject);
        applyWhereExpression(query);

        List<T> list = query.fetch();

        detachAll();

        return list;
    }

    /**
     * Finds the first instance.
     *
     * <p>
     *     Because there is no ordering it is not defined which instance will be returned. Should be used if there
     *     is only one instance or in unit tests.
     * </p>
     * @return Single instance or null if no instance exists
     */
    public T findAny() {
        JPAQuery<T> query = query().select(qObject);
        applyWhereExpression(query);

        T entity = query.fetchFirst();

        detachAll();

        return entity;
    }

    /**
     * Finds the single instance with provided id.
     *
     * @param id Id of instance to find
     * @return Single instance or null if not found
     */
    public T find(String id) {
        StringPath idPath = propertyPath("id");

        JPAQuery<T> query = query().select(qObject).where(idPath.eq(id));
        applyWhereExpression(query);

        T entity = query.fetchFirst();

        detachAll();

        return entity;
    }

    /**
     * Creates or updates instance.
     *
     * <p>
     *     Corresponds to {@link EntityManager#merge(Object)} method.
     * </p>
     * @param entity Instance to save
     * @return Saved detached instance
     * @throws IllegalArgumentException If entity is NULL
     */
    public T save(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity");
        }

        T obj = entityManager.merge(entity);

        entityManager.flush();
        detachAll();

        return obj;
    }

    /**
     * Provides batching for {@link DomainDao#save(DomainObject)} method.
     *
     * @param entities Instances to save
     * @throws IllegalArgumentException If entity is NULL
     */
    public void save(Collection<? extends T> entities) {
        if (entities == null) {
            throw new IllegalArgumentException("entities");
        }

        entities.forEach(entityManager::merge);

        entityManager.flush();
        detachAll();
    }

    /**
     * Deletes an instance.
     *
     * <p>
     *     Non existing instance is silently skipped.
     * </p>
     *
     * @param entity Instance to delete
     * @throws IllegalArgumentException If entity is NULL
     */
    public void delete(T entity) {
        if (!entityManager.contains(entity) && entity != null) {
            entity = entityManager.find(type, entity.getId());
        }

        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    /**
     * Creates QueryDSL query object.
     *
     * @return Query object
     */
    protected JPAQuery<?> query() {
        return queryFactory.from(qObject);
    }

    /**
     * Creates QueryDSL query object for other entity than the store one.
     *
     * @return Query object
     */
    protected <C> JPAQuery<?> query(EntityPathBase<C> base) {
        return queryFactory.from(base);
    }

    protected void detachAll() {
        entityManager.clear();
    }

    /**
     * Creates meta object attribute.
     *
     * <p>
     *     Used for addressing QueryDSL attributes, which are not known at compile time. Should be used with caution,
     *     because it circumvents type safety.
     * </p>
     *
     * @param name Name of the attribute
     * @return Meta object attribute
     */
    protected StringPath propertyPath(String name) {
        PathBuilder<T> builder = new PathBuilder<>(qObject.getType(), qObject.getMetadata().getName());
        return builder.getString(name);
    }

    /**
     * Provides extension point for inheriting classes to define a where clause for all find* methods
     * in {@link DomainDao}.
     *
     * @return A where clause or null
     */
    protected BooleanExpression findWhereExpression() {
        return null;
    }

    private void applyWhereExpression(JPAQuery<T> query) {
        BooleanExpression expression = findWhereExpression();

        if (expression != null) {
            query.where(expression);
        }
    }

    private Q constructQObject(Class<T> type, Class<Q> qType) {
        String name = type.getSimpleName();

        try {
            Constructor<Q> constructor = qType.getConstructor(String.class);
            return constructor.newInstance(name);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException("Error creating Q object for + " + type.getName());
        }
    }

    public T getReference(String id) {
        return entityManager.getReference(type, id);
    }

    @Inject
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Inject
    public void setQueryFactory(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }
}
