package cz.inqool.eas.common.settings.named;

import cz.inqool.eas.common.authored.user.UserGenerator;
import cz.inqool.eas.common.authored.user.UserReference;
import cz.inqool.eas.common.exception.ForbiddenObject;
import cz.inqool.eas.common.exception.ForbiddenOperation;
import cz.inqool.eas.common.exception.MissingObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;

import java.util.List;

import static cz.inqool.eas.common.utils.AssertionUtils.eq;
import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

public class NamedSettingsService {
    private NamedSettingsStore store;

    @Transactional
    public List<NamedSettings> findSettings(String tag) {
        UserReference user = UserGenerator.generateValue();
        notNull(user, () -> new ForbiddenOperation(null));

        return store.findByUserAndTag(user.getId(), tag);
    }

    @Transactional
    public NamedSettings create(NamedSettingsCreate settings) {
        UserReference user = UserGenerator.generateValue();
        notNull(user, () -> new ForbiddenOperation(null));

        NamedSettings entity = NamedSettingsCreate.toEntity(settings);
        entity.setUser(user);
        return store.create(entity);
    }

    @Transactional
    public void delete(String id) {
        UserReference user = UserGenerator.generateValue();
        notNull(user, () -> new ForbiddenOperation(null));

        NamedSettings settings = store.find(id);
        notNull(settings, () -> new MissingObject(NamedSettings.class, id));
        eq(settings.getUser(), user, () -> new ForbiddenObject(settings, ForbiddenObject.ErrorCode.FORBIDDEN));

        store.delete(id);
    }

    @Autowired
    public void setStore(NamedSettingsStore store) {
        this.store = store;
    }
}
