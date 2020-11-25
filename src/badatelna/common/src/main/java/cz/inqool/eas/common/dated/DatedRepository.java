package cz.inqool.eas.common.dated;

import cz.inqool.eas.common.dated.index.DatedIndex;
import cz.inqool.eas.common.dated.store.DatedStore;
import cz.inqool.eas.common.domain.DomainRepository;

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
}
