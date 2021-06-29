package cz.inqool.eas.common.export.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.inqool.eas.common.authored.AuthoredService;
import cz.inqool.eas.common.exception.*;
import cz.inqool.eas.common.export.access.ExportAccessChecker;
import cz.inqool.eas.common.export.event.ExportFinishedEvent;
import cz.inqool.eas.common.export.event.ExportReleasedEvent;
import cz.inqool.eas.common.export.event.ExportAcquiredEvent;
import cz.inqool.eas.common.export.request.dto.FinishProcessingDto;
import cz.inqool.eas.common.export.request.dto.SignalErrorDto;
import cz.inqool.eas.common.storage.file.File;
import cz.inqool.eas.common.storage.file.FileManager;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.SerializationUtils;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cz.inqool.eas.common.exception.ForbiddenOperation.ErrorCode.NOT_ALLOWED;
import static cz.inqool.eas.common.utils.AssertionUtils.*;
import static cz.inqool.eas.common.utils.CollectionUtils.merge;

@Slf4j
public class ExportRequestService extends AuthoredService<
        ExportRequest,
        ExportRequestDetail,
        ExportRequestList,
        ExportRequestCreate,
        ExportRequestUpdate,
        ExportRequestRepository
        > {
    private ExportAccessChecker accessChecker;

    private FileManager fileManager;

    @Setter
    private int forgottenTimeLimit = 3600;

    /**
     * fixme: check permission
     * @return
     */
    public synchronized ExportRequest acquire() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        ExportRequest exportRequest = transactionTemplate.execute(status -> {
                    ExportRequest request = repository.findNextToProcess();

                    if (request == null) {
                        return null;
                    }

                    request.setProcessingStart(Instant.now());
                    request.setState(ExportRequestState.PROCESSING);

                    log.debug("Start processing {}.", request);

                    return repository.update(request);
                }
        );

        if (exportRequest != null && !exportRequest.isSystemRequest()) {
            eventPublisher.publishEvent(new ExportAcquiredEvent(this, exportRequest));
        }

        return exportRequest;
    }

    /**
     * fixme: check permission
     * @return
     */
    public synchronized ExportRequest acquireExact(String id) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        ExportRequest exportRequest = transactionTemplate.execute(status -> {
                    ExportRequest request = repository.find(id);

                    if (request == null) {
                        return null;
                    }

                    request.setProcessingStart(Instant.now());
                    request.setState(ExportRequestState.PROCESSING);

                    log.debug("Start processing {}.", request);

                    return repository.update(request);
                }
        );

        if (exportRequest != null && !exportRequest.isSystemRequest()) {
            eventPublisher.publishEvent(new ExportAcquiredEvent(this, exportRequest));
        }

        return exportRequest;
    }

    public synchronized Integer releaseForgotten() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        List<ExportRequest> exportRequests = transactionTemplate.execute(status -> {
                    List<ExportRequest> requests = repository.listForgotten(forgottenTimeLimit);

                    List<ExportRequest> updated = requests.stream().peek(request -> {
                        request.setProcessingStart(null);
                        request.setState(ExportRequestState.PENDING);

                    }).collect(Collectors.toList());

                    updated.forEach(request -> {
                        log.debug("Released processing lock on forgotten {}.", request);
                    });

                    repository.update(updated);

                    return updated;
                }
        );

        if (exportRequests != null) {
            exportRequests.forEach(exportRequest -> {
                if (exportRequest != null && !exportRequest.isSystemRequest()) {
                    eventPublisher.publishEvent(new ExportReleasedEvent(this, exportRequest));
                }
            });

            return exportRequests.size();
        } else {
            return 0;
        }
    }

    public synchronized void finishProcessing(String id, FinishProcessingDto dto) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        ExportRequest exportRequest = transactionTemplate.execute(status -> {
                    ExportRequest request = repository.find(id);
                    notNull(request, () -> new MissingObject(ExportRequest.class, id));
                    eq(request.getState(), ExportRequestState.PROCESSING, () -> new ForbiddenOperation(request, NOT_ALLOWED));

                    File result = fileManager.get(dto.getFileId());
                    notNull(request, () -> new MissingObject(File.class, dto.getFileId()));
                    result = fileManager.storeFromUpload(result);

                    request.setProcessingEnd(Instant.now());
                    request.setState(ExportRequestState.PROCESSED);
                    request.setResult(result);

                    log.debug("End processing {}.", request);

                    return repository.update(request);
                }
        );

        if (exportRequest != null && !exportRequest.isSystemRequest()) {
            eventPublisher.publishEvent(new ExportFinishedEvent(this, exportRequest));
        }
    }

    public synchronized void signalError(String id, SignalErrorDto dto) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        ExportRequest exportRequest = transactionTemplate.execute(status -> {
                    ExportRequest request = repository.find(id);
                    notNull(request, () -> new MissingObject(ExportRequest.class, id));
                    eq(request.getState(), ExportRequestState.PROCESSING, () -> new ForbiddenOperation(request, NOT_ALLOWED));

                    request.setProcessingEnd(Instant.now());
                    request.setState(ExportRequestState.FAILED);
                    request.setMessage(dto.getMessage());

                    log.error("End processing {} with error.", request);

                    return repository.update(request);
                }
        );

        if (exportRequest != null && !exportRequest.isSystemRequest()) {
            eventPublisher.publishEvent(new ExportFinishedEvent(this, exportRequest));
        }
    }

    @Override
    protected void preCreateHook(@NotNull ExportRequest object) {
        super.preCreateHook(object);
        notNull(object.getTemplate(), () -> new MissingAttribute(object, "template", null));


        if (!object.isSystemRequest()) {
            ExportAccessChecker.AccessCheckResult access = accessChecker.checkAccess(object.getTemplate());
            isTrue(access.isAccess(), () -> new ForbiddenObject(object.getTemplate(), ForbiddenObject.ErrorCode.FORBIDDEN));

            String configuration = mergeConfigurations(object.getConfiguration(), access.getConfiguration());
            object.setConfiguration(configuration);
        }

        object.setState(ExportRequestState.PENDING);

        SecurityContext securityContext = SecurityContextHolder.getContext();

        byte[] serialized = SerializationUtils.serialize(securityContext);
        object.setSecurityContext(serialized);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected String mergeConfigurations(String first, String second) {
        if (first == null && second == null) {
            return null;
        } else if (first == null) {
            return second;
        } else if (second == null) {
            return first;
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            Map firstMap = mapper.readValue(first, Map.class);
            Map secondMap = mapper.readValue(second, Map.class);

            Map merged = merge(firstMap, secondMap);

            return mapper.writeValueAsString(merged);
        } catch (JsonProcessingException e) {
            throw new GeneralException("Failed to merge configurations", e);
        }
    }

    @Autowired
    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Autowired
    public void setAccessChecker(ExportAccessChecker accessChecker) {
        this.accessChecker = accessChecker;
    }
}
