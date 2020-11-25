package cz.inqool.eas.common.intl;

import cz.inqool.eas.common.dictionary.DictionaryRepository;
import cz.inqool.eas.common.dictionary.index.DictionaryIndex;
import cz.inqool.eas.common.dictionary.store.DictionaryStore;


public class TranslationRepository extends DictionaryRepository<
        Translation,
        Translation,
        TranslationIndexedObject,
        DictionaryStore<Translation, Translation, QTranslation>,
        DictionaryIndex<Translation, Translation, TranslationIndexedObject>> {

    public Translation findFirstByLanguage(Language language) {
        QTranslation model = QTranslation.translation;

        Translation translation = query().
                select(model).
                from(model).
                where(model.deleted.isNull()).
                where(model.language.eq(language)).
                orderBy(model.order.asc()).
                fetchFirst();

        detachAll();

        return translation;
    }
}
