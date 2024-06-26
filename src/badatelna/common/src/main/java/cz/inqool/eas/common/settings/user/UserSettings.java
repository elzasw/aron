package cz.inqool.eas.common.settings.user;

import cz.inqool.eas.common.authored.user.UserGenerator;
import cz.inqool.eas.common.authored.user.UserReference;
import cz.inqool.eas.common.dated.store.DatedObject;
import cz.inqool.eas.common.domain.DomainViews;
import cz.inqool.entityviews.ViewableProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;

import javax.persistence.*;

import static cz.inqool.eas.common.domain.DomainViews.DEFAULT;

@DomainViews
@Setter
@Getter
@Entity
@Table(name = "eas_user_settings")
public class UserSettings extends DatedObject<UserSettings> {
    /**
     * JSON settings
     */
    protected String settings;

    @ViewableProperty(views = DEFAULT)
    @GeneratorType( type = UserGenerator.class, when = GenerationTime.INSERT)
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "user_id"))
    @AttributeOverride(name = "name", column = @Column(name = "user_name"))
    protected UserReference user;

}
