package cz.inqool.eas.common.admin.console.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for admin console subsystem.
 * <p>
 * If application wants to use admin console, it needs to extend this class and add {@link Configuration} annotation.
 */
@Slf4j
public abstract class AdminConsoleConfiguration implements WebMvcConfigurer {

    /**
     * Exposes static web content for admin console sources.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/console/**").addResourceLocations("classpath:/console/");
    }
}
