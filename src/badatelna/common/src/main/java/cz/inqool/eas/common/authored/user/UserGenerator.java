package cz.inqool.eas.common.authored.user;

import cz.inqool.eas.common.authored.store.AuthoredObject;
import cz.inqool.eas.common.security.User;
import org.hibernate.Session;
import org.hibernate.tuple.ValueGenerator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Class for hibernate value generating used in {@link AuthoredObject}
 * get {@link UserReference} from ApplicationContext, so it's always currently logged in User
 *
 */
public class UserGenerator implements ValueGenerator<UserReference> {
    @Override
    public UserReference generateValue(Session session, Object o) {
        return generateValue();
    }

    public static UserReference generateValue() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                User user = (User) principal;
                return new UserReference(user.getId(), user.getName());
            }
        }

        return null;
    }
}
