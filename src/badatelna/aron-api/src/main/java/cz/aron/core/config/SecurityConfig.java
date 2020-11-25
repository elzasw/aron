package cz.aron.core.config;

import cz.inqool.eas.common.security.BaseSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * @author Lukas Jane (inQool) 02.11.2020.
 */
@Configuration
public class SecurityConfig extends BaseSecurityConfiguration {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();  //or testing POST from swagger does not work
        //we kill spring oauth autoconfig and all security
    }
}
