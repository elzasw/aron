package cz.inqool.eas.common.domain.index.reindex;

import cz.inqool.eas.common.domain.DomainRepository;
import cz.inqool.eas.common.utils.AopUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ReindexService {
    private List<DomainRepository<?, ?, ?, ?, ?>> repositories;

    @Setter
    private Function<SecurityExpressionRoot, Boolean> accessChecker;


    @PreAuthorize("this.canAccess(#root)")
    public List<Class<?>> getRepositories() {
        return repositories.stream().map(Object::getClass).collect(Collectors.toList());

    }

    @Transactional
    @PreAuthorize("this.canAccess(#root)")
    public void reindex(List<String> storeClasses) {
        List<DomainRepository<?, ?, ?, ?, ?>> repositories = this.repositories.stream()
                .filter(store -> storeClasses == null || storeClasses.isEmpty() || storeClasses.contains(store.getClass().getName()))
                .collect(Collectors.toList());

        int counter = 0;
        int total = repositories.size();
        for (var repository : repositories) {
            log.info("Reindexing repository {}/{}: {}", ++counter, total, repository);

            if (repository.isIndexInitialized()) {
                log.info("Found existing index {}.", repository);
                log.info("Dropping existing index.");
                repository.dropIndex();
            }

            log.info("Indexing {}.", repository);
            repository.reindex();
        }

        log.info("Reindexing complete");
    }

    public boolean canAccess(SecurityExpressionRoot root) {
        return accessChecker.apply(root);
    }

    @Autowired(required = false)
    public void setRepositories(List<DomainRepository<?, ?, ?, ?, ?>> repositories) {
        this.repositories = repositories.stream()
                .map(AopUtils::unwrap)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
