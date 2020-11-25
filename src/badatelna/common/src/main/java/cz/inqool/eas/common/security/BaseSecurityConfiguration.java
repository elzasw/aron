package cz.inqool.eas.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

public abstract class BaseSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .and()
            .exceptionHandling()
                .and()
            .headers()
                .cacheControl()
                    .and()
                .frameOptions()
                    .disable()
                    .and()
            .authorizeRequests()
                .antMatchers("/api-docs/**").permitAll()        // allow swagger
                .antMatchers("/me").permitAll()                 // allow MeApi
                .antMatchers("/translations/load/*").permitAll()    // allow translations, todo: configurable url support
                .and();
    }
}
