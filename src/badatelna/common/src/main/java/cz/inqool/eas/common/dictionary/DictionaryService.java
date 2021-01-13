package cz.inqool.eas.common.dictionary;

import cz.inqool.eas.common.authored.AuthoredService;
import cz.inqool.eas.common.dictionary.index.DictionaryAutocomplete;
import cz.inqool.eas.common.domain.index.dto.Result;
import cz.inqool.eas.common.domain.index.dto.params.Params;
import cz.inqool.eas.common.intl.Language;
import cz.inqool.eas.common.projection.Projectable;

import javax.annotation.Nullable;
import javax.transaction.Transactional;
import java.util.List;

/**
 * CRUD Service layer for objects implementing {@link Dictionary}.
 *
 * @param <ROOT> Root of the projection type system
 * @param <DETAIL_PROJECTION> Projection used for retrieving object detail
 * @param <LIST_PROJECTION> Projection used for listing object
 * @param <CREATE_PROJECTION> Projection used as DTO for object creation
 * @param <UPDATE_PROJECTION> Projection used as DTO for object update
 * @param <REPOSITORY> Repository type
 */
public abstract class DictionaryService<
        ROOT extends Dictionary<ROOT>,
        DETAIL_PROJECTION extends Dictionary<ROOT>,
        LIST_PROJECTION extends Dictionary<ROOT>,
        CREATE_PROJECTION extends Projectable<ROOT>,
        UPDATE_PROJECTION extends Projectable<ROOT>,
        REPOSITORY extends DictionaryRepository<ROOT, ?, ?, ?, ?>
        >
        extends AuthoredService<
        ROOT,
        DETAIL_PROJECTION,
        LIST_PROJECTION,
        CREATE_PROJECTION,
        UPDATE_PROJECTION,
        REPOSITORY> {

    public Result<DictionaryAutocomplete> listAutocomplete(@Nullable String query, @Nullable Language language, @Nullable Params params) {
        return this.repository.listAutocomplete(query, language, params, false);
    }

    public Result<DictionaryAutocomplete> listAutocompleteAll(@Nullable String query, @Nullable Language language, @Nullable Params params) {
        return this.repository.listAutocomplete(query, language, params, true);
    }

    public List<DictionaryAutocomplete> listAutocompleteFull(@Nullable String query, @Nullable Language language, @Nullable Params params) {
        return this.repository.listAutocompleteFull(query, language, params, false);
    }

    public List<DictionaryAutocomplete> listAutocompleteFullAll(@Nullable String query, @Nullable Language language, @Nullable Params params) {
        return this.repository.listAutocompleteFull(query, language, params, true);
    }

    /**
     * Activates object.
     *
     * @param id Id of object to activate
     */
    @Transactional
    public void activate(String id) {
        repository.activate(id);
    }

    /**
     * Deactivates object.
     * @param id Id of object to deactivate
     */
    @Transactional
    public void deactivate(String id) {
        repository.deactivate(id);
    }
}
