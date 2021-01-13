package cz.inqool.eas.common.dictionary.store;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQuery;
import cz.inqool.eas.common.authored.store.AuthoredStore;
import cz.inqool.eas.common.dictionary.Dictionary;
import cz.inqool.eas.common.exception.MissingObject;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Collection;

import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

/**
 * Database store for objects extending {@link DictionaryObject} with standard CRUD operations.
 *
 *
 * @param <ROOT> Entity type
 * @param <PROJECTED> Entity projection type
 * @param <META_MODEL> Meta model for projection type
 */
public class DictionaryStore<
        ROOT extends Dictionary<ROOT>,
        PROJECTED extends Dictionary<ROOT>,
        META_MODEL extends EntityPathBase<PROJECTED>
        > extends AuthoredStore<
        ROOT,
        PROJECTED,
        META_MODEL
        > {
    protected final QDictionaryObject dictionaryMetaModel;

    public DictionaryStore(Class<PROJECTED> type) {
        super(type);

        this.dictionaryMetaModel = new QDictionaryObject(this.metaModel.getMetadata());
    }

    /**
     * Activates object.
     * @param id Id of object to activate
     */
    public void activate(String id) {
        PROJECTED projected = this.find(id);
        notNull(projected, () -> new MissingObject(this.getType(), id));

        projected.setActive(true);
        this.update(projected);
    }

    /**
     * Disables object.
     * @param id Id of object to disable
     */
    public void deactivate(String id) {
        PROJECTED projected = this.find(id);
        notNull(projected, () -> new MissingObject(this.getType(), id));

        projected.setActive(false);
        this.update(projected);
    }

    /**
     * Finds dictionary object based on code.
     *
     * @param code Code of object
     * @return Found object or null
     */
    public PROJECTED findByCode(@NotNull String code) {
        Instant now = Instant.now();

        JPAQuery<PROJECTED> query = query().
                select(metaModel).
                from(metaModel)
                .where(dictionaryMetaModel.code.eq(code))
                .where(dictionaryMetaModel.deleted.isNull())
                .where(dictionaryMetaModel.active.isTrue())
                .where(dictionaryMetaModel.validFrom.isNull().or(dictionaryMetaModel.validFrom.loe(now)))
                .where(dictionaryMetaModel.validTo.isNull().or(dictionaryMetaModel.validTo.gt(now)));

        PROJECTED projected = query.fetchFirst();

        detachAll();

        return projected;
    }

    /**
     * Returns all active instances.
     *
     * @return collection of found instances
     */
    public Collection<PROJECTED> listActive() {
        return listDefault((query) -> query.where(dictionaryMetaModel.active.eq(true)));
    }
}
