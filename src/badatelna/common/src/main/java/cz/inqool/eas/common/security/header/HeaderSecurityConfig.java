package cz.inqool.eas.common.security.header;

import cz.inqool.eas.common.security.User;
import cz.inqool.eas.common.security.form.internal.FormLoginRedirectStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * TODO: comment
 */
@Slf4j
public abstract class HeaderSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(this.getAuthenticationUserDetailsService());
        auth.authenticationProvider(provider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        FormLoginRedirectStrategy redirectStrategy = new FormLoginRedirectStrategy();
        redirectStrategy.setContextRelative(true);

        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setAuthenticationManager(this.authenticationManagerBean());
        filter.setInvalidateSessionOnPrincipalChange(true);
        filter.setCheckForPrincipalChanges(true);
        filter.setPrincipalRequestHeader(this.getUsernameHeader());

        filter.setAuthenticationSuccessHandler((request, response, authentication) -> {
            User user = (User) authentication.getPrincipal();
            log.info("User '{}' logged in.", user.getUsername());

            redirectStrategy.sendRedirect(request, response, getSuccessRedirectUrl());
        });
        filter.setAuthenticationFailureHandler((request, response, exception) -> {
            log.info("User failed to log in. ({})", exception.getMessage());
            log.debug("Exception: ", exception);
        });

        http
                .requestMatcher(new AntPathRequestMatcher(getAuthUrl()))
                .csrf().disable()
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .anyRequest().authenticated()
                )
                .exceptionHandling()
                .and()
                .addFilterAfter(filter, LogoutFilter.class)
                .addFilterAfter((request, response, chain) -> {
                    // do nothing, so this request ends here
                }, RequestHeaderAuthenticationFilter.class);
    }

    protected abstract String getAuthUrl();

    protected abstract String getSuccessRedirectUrl();

    protected abstract String getUsernameHeader();

    protected abstract AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> getAuthenticationUserDetailsService();
}
