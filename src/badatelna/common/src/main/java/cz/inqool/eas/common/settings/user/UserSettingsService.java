package cz.inqool.eas.common.settings.user;

import cz.inqool.eas.common.authored.user.UserGenerator;
import cz.inqool.eas.common.authored.user.UserReference;
import cz.inqool.eas.common.exception.ForbiddenOperation;
import cz.inqool.eas.common.exception.MissingObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;

import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

public class UserSettingsService {
    private UserSettingsStore store;

    @Transactional
    public String findUserSettings() {
        UserReference user = UserGenerator.generateValue();
        notNull(user, () -> new ForbiddenOperation(null));

        UserSettings userSettings = store.findBy(user.getId());
        if (userSettings == null) {
            userSettings = new UserSettings();
            userSettings.setUser(user);
            userSettings = store.create(userSettings);
        }
        return userSettings.getSettings();
    }

    @Transactional
    public UserSettings update(String settings) {
        UserReference user = UserGenerator.generateValue();
        UserSettings entity = store.findBy(user.getId());
        notNull(entity, () -> new MissingObject(UserSettings.class));
        entity.setSettings(settings);
        return store.update(entity);
    }

    @Transactional
    public void clear() {
        UserReference user = UserGenerator.generateValue();
        UserSettings entity = store.findBy(user.getId());
        notNull(entity, () -> new MissingObject(UserSettings.class));
        entity.setSettings("{}");
        store.update(entity);
    }

    @Autowired
    public void setStore(UserSettingsStore store) {
        this.store = store;
    }
}
