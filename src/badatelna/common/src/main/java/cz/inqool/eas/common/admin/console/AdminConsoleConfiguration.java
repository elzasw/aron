package cz.inqool.eas.common.admin.console;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Configuration for admin console subsystem.
 * <p>
 * If application wants to use admin console, it needs to extend this class and add {@link Configuration} annotation.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public abstract class AdminConsoleConfiguration {
    @Bean
    public AdminConsoleService adminConsoleService() {
        return new AdminConsoleService();
    }

    @Bean
    public AdminConsoleOutputRouter adminConsoleOutputRouter() {
        return new AdminConsoleOutputRouter();
    }
}
