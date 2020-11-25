package cz.inqool.eas.common.security.form;

import cz.inqool.eas.common.security.User;
import cz.inqool.eas.common.security.captcha.CaptchaValidator;
import cz.inqool.eas.common.security.form.internal.FormLoginConfigurer;
import cz.inqool.eas.common.security.form.internal.FormLoginEntryPoint;
import cz.inqool.eas.common.security.form.internal.FormLoginRedirectStrategy;
import cz.inqool.eas.common.security.form.internal.UsernamePasswordCaptchaAuthenticationFilter;
import cz.inqool.eas.common.security.internal.CombinedFailureHandler;
import cz.inqool.eas.common.security.internal.CombinedSuccessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * TODO: comment
 */
@Slf4j
public abstract class FormSecurityConfig extends WebSecurityConfigurerAdapter {
    private CaptchaValidator captchaValidator;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        log.info("Initializing FormSecurityConfig for " + this.getClass().getName());
        auth.authenticationProvider(this.getAuthenticationProvider());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        FormLoginConfigurer<HttpSecurity> formLoginConfigurer = new FormLoginConfigurer<>();

        FormLoginRedirectStrategy redirectStrategy = new FormLoginRedirectStrategy();
        redirectStrategy.setContextRelative(true);

        FormLoginEntryPoint entryPoint = new FormLoginEntryPoint(this.getLoginPage());
        entryPoint.setRedirectStrategy(redirectStrategy);

        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler(this.getLoginPage()+"?error");
        failureHandler.setRedirectStrategy(redirectStrategy);

        UsernamePasswordAuthenticationFilter authenticationFilter;
        if (captchaValidator != null) {
            log.info("Enabling captcha validator");
            authenticationFilter = UsernamePasswordCaptchaAuthenticationFilter
                    .builder()
                    .validator(captchaValidator)
                    .action(getCaptchaAction())
                    .build();
        } else {
            authenticationFilter = new UsernamePasswordAuthenticationFilter();
        }

        http
                .requestMatcher(new AntPathRequestMatcher(this.getAuthUrl()))
                .csrf().disable()
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .anyRequest().authenticated()
                )
                .exceptionHandling()
                .authenticationEntryPoint(entryPoint)
                .and()
                .apply(formLoginConfigurer)
                .authenticationFilter(authenticationFilter)
                .loginPage(this.getLoginPage())
                .failureHandler(new CombinedFailureHandler(
                        failureHandler,
                        (request, response, exception) -> {
                            log.info("User failed to log in. ({})", exception.getMessage());
                            log.debug("Exception: ", exception);
                        }
                ))
                .successHandler(new CombinedSuccessHandler(
                        (request, response, authentication) -> {
                            User user = (User) authentication.getPrincipal();
                            log.info("User '{}' logged in.", user.getUsername());

                            redirectStrategy.sendRedirect(request, response, getSuccessRedirectUrl());
                        }
                ))
                .loginProcessingUrl(this.getAuthUrl())
                .and();
    }

    @Override
    @Bean("authenticationManager")
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Autowired(required = false)
    public void setCaptchaValidator(CaptchaValidator captchaValidator) {
        this.captchaValidator = captchaValidator;
    }

    /**
     * Override if default CAPTCHA action is not suitable.
     * @return
     */
    protected String getCaptchaAction() {
        return "login";
    }

    protected abstract String getLoginPage();
    protected abstract String getSuccessRedirectUrl();
    protected abstract String getAuthUrl();

    protected abstract AuthenticationProvider getAuthenticationProvider();
}
