package cz.inqool.eas.common.domain.index;

import cz.inqool.eas.common.domain.DomainRepository;
import cz.inqool.eas.common.domain.index.reindex.ReindexService;
import cz.inqool.eas.common.domain.index.reindex.queue.ReindexQueue;
import cz.inqool.eas.common.domain.index.reindex.queue.ReindexQueueStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Automatically check after startup if all indexes are
 * created and if not, create the missing ones.
 */
@Slf4j
@Component
public class IndexInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private List<DomainRepository<?, ?, ?, ?, ?>> repositories;
    private ReindexQueueStore<? extends ReindexQueue<?>> reindexQueueStore;
    private ReindexService reindexService;
    private TransactionTemplate transactionTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (repositories != null) {
            log.info("Checking for missing indexes.");

            boolean atLeastOneFound = false;

            for (var repository : repositories) {
                if (!repository.isIndexInitialized()) {
                    atLeastOneFound = true;
                    log.info("Found missing index for {}.", repository);
                    log.info("Recreating missing index.");
                    repository.initIndex();
                }
            }

            if (atLeastOneFound) {
                log.info("All missing indexes were created.");
            } else {
                log.info("No missing indexes found.");
            }

            log.info("Checking for repositories to be reindexed.");
            Boolean atLeastOneReindexed = false;

            if (reindexService != null && reindexQueueStore != null) {
                for (ReindexQueue<? extends ReindexQueue<?>> reindexQueueItem : reindexQueueStore.listAll()) {
                    atLeastOneReindexed |= transactionTemplate.execute(status -> {
                        var repositoryClass = reindexQueueItem.getRepositoryClass();
                        log.info("Found {} repository queued for reindex", repositoryClass);
                        reindexService.reindex(List.of(repositoryClass.getName()));
                        reindexQueueStore.delete(reindexQueueItem.getId());
                        return true;
                    });
                }
            }

            if (atLeastOneReindexed) {
                log.info("All queued repositories were reindexed.");
            } else {
                log.info("No repository queued for reindex.");
            }
        }
    }

    @Autowired(required = false)
    public void setRepositories(List<DomainRepository<?, ?, ?, ?, ?>> repositories) {
        this.repositories = repositories;
    }

    @Autowired(required = false)
    public void setReindexQueueStore(ReindexQueueStore<?> store) {
        this.reindexQueueStore = store;
    }

    @Autowired(required = false)
    public void setReindexService(ReindexService service) {
        this.reindexService = service;
    }

    @Autowired
    public void setTransactionTemplate(TransactionTemplate template) {
        this.transactionTemplate = template;
    }
}
