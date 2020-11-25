package cz.inqool.eas.common.intl;

import cz.inqool.eas.common.dictionary.DictionaryService;
import cz.inqool.eas.common.storage.file.FileManager;
import cz.inqool.eas.common.storage.file.OpenedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nonnull;

@Slf4j
public class TranslationService extends DictionaryService<
        Translation,
        TranslationDetail,
        TranslationList,
        TranslationCreate,
        TranslationUpdate,
        TranslationRepository
        > {

    private FileManager fileManager;

    public OpenedFile load(Language language) {
        Translation translation = repository.findFirstByLanguage(language);
        if (translation == null || translation.getContent() == null) {
            log.warn("No translation for language {} found.", language);
            return null;
        }

        return fileManager.open(translation.getId());
    }

    @Override
    protected void preCreateHook(@Nonnull Translation translation) {
        super.preCreateHook(translation);

        fileManager.preCreateHook(translation, Translation::getContent);
    }

    @Override
    protected void preUpdateHook(@Nonnull Translation translation) {
        super.preUpdateHook(translation);

        Translation oldTranslation = getInternal(Translation.class, translation.getId());
        fileManager.preUpdateHook(translation, oldTranslation, Translation::getContent);
    }

    @Override
    protected void preDeleteHook(@Nonnull String id) {
        super.preDeleteHook(id);

        Translation translation = getInternal(Translation.class, id);
        fileManager.preDeleteHook(translation, Translation::getContent);
    }

    @Autowired
    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }
}
