package cz.aron.core.config.reindex;

import cz.inqool.eas.common.domain.index.reindex.ReindexConfiguration;
import cz.inqool.eas.common.domain.index.reindex.ReindexService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Lukas Jane (inQool) 09.11.2020.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReindexConfig extends ReindexConfiguration {
    @Override
    protected String reindexUrl() {
        return "debug/reindex";
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ReindexService reindexService() {
        return new ReindexService();
    }
}
