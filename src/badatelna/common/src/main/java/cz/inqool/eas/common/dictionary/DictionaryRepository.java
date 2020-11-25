package cz.inqool.eas.common.dictionary;

import cz.inqool.eas.common.authored.AuthoredRepository;
import cz.inqool.eas.common.dated.index.DatedIndexedObject;
import cz.inqool.eas.common.dictionary.index.DictionaryAutocomplete;
import cz.inqool.eas.common.dictionary.index.DictionaryIndex;
import cz.inqool.eas.common.dictionary.index.DictionaryIndexedObject;
import cz.inqool.eas.common.dictionary.index.DictionaryIndexedObject.Fields;
import cz.inqool.eas.common.dictionary.store.DictionaryStore;
import cz.inqool.eas.common.domain.index.dto.Result;
import cz.inqool.eas.common.domain.index.dto.filter.*;
import cz.inqool.eas.common.domain.index.dto.params.Params;
import cz.inqool.eas.common.domain.index.dto.sort.FieldSort;
import cz.inqool.eas.common.intl.Language;
import org.elasticsearch.search.sort.SortOrder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

/**
 * Repository for objects implementing {@link Dictionary} interface.
 *
 * Database and index functionality combined together.
 *
 * @param <ROOT> Root of the projection type system
 * @param <INDEX_PROJECTED> Index projection type
 * @param <INDEXED> Indexed object type
 * @param <STORE> Type of store
 * @param <INDEX> Type of index
 */
public class DictionaryRepository<
        ROOT extends Dictionary<ROOT>,
        INDEX_PROJECTED extends Dictionary<ROOT>,
        INDEXED extends DictionaryIndexed<ROOT, INDEX_PROJECTED>,
        STORE extends DictionaryStore<ROOT, ROOT, ?>,
        INDEX extends DictionaryIndex<ROOT, INDEX_PROJECTED, INDEXED>
        > extends AuthoredRepository<
                ROOT,
                INDEX_PROJECTED,
                INDEXED,
                STORE,
                INDEX> {

    /**
     * Activates object.
     * @param id Id of object to activate
     */
    public void activate(String id) {
        getStore().activate(id);
        getIndex().activate(id);
    }

    /**
     * @see DictionaryStore#findByCode(String) (String)
     */
    public <PROJECTED extends Dictionary<ROOT>> PROJECTED findByCode(Class<PROJECTED> type, @Nonnull String code) {
        return getDictionaryStore(type).findByCode(code);
    }


    /**
     * Deactivates object.
     * @param id Id of object to deactivate
     */
    public void deactivate(String id) {
        getStore().deactivate(id);
        getIndex().deactivate(id);
    }

    /**
     * Retrieves objects using provided projection type and query string.
     * <p>
     * Used for autocomplete endpoints.
     *
     * @param query  Query string
     * @param params Default params
     */
    public Result<DictionaryAutocomplete> listAutocomplete(@Nullable String query, @Nullable Language language, @Nullable Params params) {
        params = prepareAutocompleteParams(params, language, query);

        return this.getIndex().listByParams(params, (builder) -> builder.fetchSource(new String[]{Fields.name, Fields.multiName, Fields.code}, null), (hit) -> {
            String value = (String) hit.getSourceAsMap().get(Fields.name);
            if (language != null) {
                value = ((HashMap<Language, String>) hit.getSourceAsMap().get(Fields.multiName)).get(language.name().toLowerCase());
            }
            String code = (String) hit.getSourceAsMap().get(Fields.code);

            return new DictionaryAutocomplete(hit.getId(), value, code);
        });
    }

    /**
     * Retrieves all objects using provided projection type and query string.
     * <p>
     * Used for autocomplete endpoints.
     *
     * @param query  Query string
     * @param params Default params
     */
    public List<DictionaryAutocomplete> listAutocompleteFull(@Nullable String query, @Nullable Language language, @Nullable Params params) {
        params = prepareAutocompleteParams(params, language, query);

        return this.getIndex().listAllByParams(params, (builder) -> builder.fetchSource(new String[]{Fields.name, Fields.multiName, Fields.code}, null), (hit) -> {
            String value = (String) hit.getSourceAsMap().get(Fields.name);
            if (language != null) {
                value = ((HashMap<Language, String>) hit.getSourceAsMap().get(Fields.multiName)).get(language.name().toLowerCase());
            }
            String code = (String) hit.getSourceAsMap().get(Fields.code);

            return new DictionaryAutocomplete(hit.getId(), value, code);
        });
    }

    private Params prepareAutocompleteParams(Params params, @Nullable Language language, @Nullable String query) {
        if (params == null) {
            params = new Params();
        }

        params.setSort(List.of(new FieldSort(Fields.order, SortOrder.ASC), new FieldSort(DictionaryIndexedObject.Fields.name, SortOrder.ASC)));

        // text filter
        if (query != null) {
            if (language == null) {
                params.addFilter(new ContainsFilter(Fields.name, query));
            } else {
                params.addFilter(new ContainsFilter(Fields.multiName + "." + language.name().toLowerCase(), query));
            }
        }

        // only active
        params.addFilter(new EqFilter(Fields.active, "true"));

        // only valid
        String now = LocalDateTime.now().toString();
        params.addFilter(new OrFilter(new NullFilter(Fields.validFrom), new LteFilter(Fields.validFrom, now)));
        params.addFilter(new OrFilter(new NullFilter(Fields.validTo), new GteFilter(Fields.validTo, now)));

        return params;
    }

    @SuppressWarnings("unchecked")
    protected <PROJECTED extends Dictionary<ROOT>> DictionaryStore<ROOT, PROJECTED, ?> getDictionaryStore(Class<PROJECTED> type) {
        return (DictionaryStore<ROOT, PROJECTED, ?>)super.getStore(type);
    }
}
