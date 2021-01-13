package cz.inqool.eas.common.dated;

import cz.inqool.eas.common.dated.index.DatedIndex;
import cz.inqool.eas.common.dated.store.DatedStore;
import cz.inqool.eas.common.domain.DomainRepository;
import cz.inqool.eas.common.projection.Projection;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Repository for objects implementing {@link Dated} interface.
 * <p>
 * Database and index functionality combined together.
 *
 * @param <ROOT>            Root of the projection type system
 * @param <INDEX_PROJECTED> Index projection type
 * @param <INDEXED>         Indexed object type
 * @param <STORE> Type of store
 * @param <INDEX> Type of index
 */
public class DatedRepository<
        ROOT extends Dated<ROOT>,
        INDEX_PROJECTED extends Dated<ROOT>,
        INDEXED extends DatedIndexed<ROOT, INDEX_PROJECTED>,
        STORE extends DatedStore<ROOT, ROOT, ?>,
        INDEX extends DatedIndex<ROOT, INDEX_PROJECTED, INDEXED>
        > extends DomainRepository<
        ROOT,
        INDEX_PROJECTED,
        INDEXED,
        STORE,
        INDEX> {

    /**
     * Permanently deletes given object from database and index.
     *
     * @param id          Id of object to delete permanently
     * @return resultant entity or {@code null} if the entity was not found
     */
    public ROOT deletePermanently(@NotNull String id) {
        //noinspection unchecked
        DatedStore<ROOT, INDEX_PROJECTED, ?> store = (DatedStore<ROOT, INDEX_PROJECTED, ?>) getStore();

        INDEX_PROJECTED projected = store.deletePermanently(id);

        if (projected != null) {
            INDEXED indexed = projectedToIndexable(indexProjectedType, projected);
            index.deletePermanently(indexed);
        }

        Projection<ROOT, ROOT, INDEX_PROJECTED> projection = projectionFactory.get(rootType, indexProjectedType);
        return projection.toBase(projected);
    }

    /**
     * Permanently deletes given collection of objects in a batch from database and index.
     *
     * @param ids         Collection of ids of objects to delete permanently
     * @return Collection of deleted objects
     */
    public Collection<ROOT> deletePermanently(@NotNull Collection<String> ids) {
        //noinspection unchecked
        DatedStore<ROOT, INDEX_PROJECTED, ?> store = (DatedStore<ROOT, INDEX_PROJECTED, ?>) getStore();

        Collection<INDEX_PROJECTED> projected = store.deletePermanently(ids);

        List<INDEXED> indexed = projectedToIndexable(indexProjectedType, projected);
        index.deletePermanently(indexed);

        Projection<ROOT, ROOT, INDEX_PROJECTED> projection = projectionFactory.get(rootType, indexProjectedType);
        return projected.stream().map(projection::toBase).collect(Collectors.toList());
    }
}
