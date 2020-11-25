package cz.inqool.eas.common.security.form;

import cz.inqool.eas.common.exception.MissingObject;
import cz.inqool.eas.common.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

public abstract class FormAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
    private PasswordEncoder passwordEncoder;

    @Override
    protected final void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        String username = (String)authentication.getPrincipal();
        String password = (String)authentication.getCredentials();

        try {
            User user = this.findUser(username);
            notNull(user, () -> new MissingObject(User.class, username));

            boolean result = passwordEncoder.matches(password, user.getPassword());

            if (result != Boolean.TRUE) {
                throw new BadCredentialsException("Invalid password supplied");
            }
        } catch (Exception e) {
            throw new AuthenticationServiceException("Error communicating with auth db", e);
        }
    }

    @Override
    protected final UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        User user = findUser(username);
        notNull(user, () -> new UsernameNotFoundException(username));

        return user;
    }

    protected abstract User findUser(String username);

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
}
