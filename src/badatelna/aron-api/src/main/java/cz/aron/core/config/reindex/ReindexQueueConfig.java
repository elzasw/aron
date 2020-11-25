package cz.aron.core.config.reindex;

import cz.inqool.eas.common.domain.index.reindex.queue.ReindexQueueConfiguration;
import cz.inqool.eas.common.domain.index.reindex.queue.ReindexQueueStore;
import org.springframework.context.annotation.Configuration;

/**
 * @author Lukas Jane (inQool) 10.11.2020.
 */
@Configuration
public class ReindexQueueConfig extends ReindexQueueConfiguration<ReindexQueueImpl> {
    @Override
    protected void configure(ReindexQueueStore.ReindexQueueStoreBuilder<ReindexQueueImpl> builder) {
        builder.type(ReindexQueueImpl.class);
    }
}
