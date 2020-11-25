package cz.inqool.eas.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Slf4j
@Order(50)
@Configuration
public class LogoutConfiguration extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .requestMatcher(new AntPathRequestMatcher("/logout"))
                .csrf().disable()
                .authorizeRequests().anyRequest().permitAll().and()
                .logout().defaultLogoutSuccessHandlerFor((request, response, authentication) -> {
                    if (authentication != null) {
                        Object principal = authentication.getPrincipal();

                        if (principal instanceof User) {
                            User user = (User) principal;
                            log.info("{} has logout.", user);
                        }
                    }
                }, new AntPathRequestMatcher("/logout"));


    }
}
