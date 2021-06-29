package cz.inqool.eas.common.signing.request;

import com.google.common.base.Objects;
import cz.inqool.eas.common.authored.AuthoredService;
import cz.inqool.eas.common.authored.user.UserReference;
import cz.inqool.eas.common.exception.BadArgument;
import cz.inqool.eas.common.exception.ForbiddenObject;
import cz.inqool.eas.common.exception.MissingAttribute;
import cz.inqool.eas.common.exception.MissingObject;
import cz.inqool.eas.common.signing.request.dto.UploadSignedContentDto;
import cz.inqool.eas.common.signing.request.event.SignCanceledEvent;
import cz.inqool.eas.common.signing.request.event.SignCompletedEvent;
import cz.inqool.eas.common.signing.request.event.SignErrorEvent;
import cz.inqool.eas.common.storage.file.File;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cz.inqool.eas.common.utils.AssertionUtils.eq;
import static cz.inqool.eas.common.utils.AssertionUtils.notNull;

public class SignRequestService extends AuthoredService<
        SignRequest,
        SignRequestDetail,
        SignRequestList,
        SignRequestCreate,
        SignRequestUpdate,
        SignRequestRepository
        > {

    @Override
    protected void preCreateHook(@NotNull SignRequest object) {
        super.preCreateHook(object);

        object.setState(SignRequestState.NEW);
    }

    @Transactional
    public SignRequest enqueue(String name, String identifier, UserReference user, Set<File> toSign) {
        SignRequest request = new SignRequest();
        request.setName(name);
        request.setIdentifier(identifier);

        LinkedHashSet<SignContent> contents = toSign.stream().map(file -> {
            SignContent content = new SignContent();
            content.setToSign(file);
            return content;
        }).collect(Collectors.toCollection(LinkedHashSet::new));

        request.setContents(contents);
        request.setUser(user);

        this.createInternal(request);
        return request;
    }

    /**
     * fixme: call from custom API
     */
    @Transactional
    public void cancel(String id) {
        SignRequest request = repository.find(id);
        notNull(request, () -> new MissingObject(SignRequest.class, id));
        eq(request.getState(), SignRequestState.NEW, () -> new ForbiddenObject(request, ForbiddenObject.ErrorCode.FORBIDDEN));

        request.setState(SignRequestState.CANCELED);
        repository.update(request);

        this.eventPublisher.publishEvent(new SignCanceledEvent(this, request));
    }

    /**
     * fixme: call from custom API
     */
    @Transactional
    public void error(String id, String error) {
        SignRequest request = repository.find(id);
        notNull(request, () -> new MissingObject(SignRequest.class, id));
        eq(request.getState(), SignRequestState.NEW, () -> new ForbiddenObject(request, ForbiddenObject.ErrorCode.FORBIDDEN));

        request.setState(SignRequestState.ERROR);
        request.setError(error);
        repository.update(request);

        this.eventPublisher.publishEvent(new SignErrorEvent(this, request));
    }

    @Transactional
    public void uploadSignedFile(String requestId, UploadSignedContentDto dto) {
        notNull(dto, BadArgument::new);
        notNull(dto.getContent(), () -> new MissingAttribute(dto, "content", null));
        notNull(dto.getSigned(), () -> new MissingAttribute(dto, "signed", null));

        uploadSignedFile(requestId, dto.getContent().getId(), dto.getSigned());
    }

    /**
     * fixme: call from custom API
     */
    @Transactional
    public void uploadSignedFile(String requestId, String contentId, File signed) {
        SignRequest request = repository.find(requestId);
        notNull(request, () -> new MissingObject(SignRequest.class, requestId));
        eq(request.getState(), SignRequestState.NEW, () -> new ForbiddenObject(request, ForbiddenObject.ErrorCode.FORBIDDEN));

        SignContent content = request.getContents()
                .stream()
                .filter(c -> Objects.equal(c.getId(), contentId))
                .findFirst()
                .orElse(null);
        notNull(content, () -> new MissingObject(SignContent.class, contentId));

        content.setSigned(signed);

        repository.update(request);
    }

    /**
     * fixme: call from custom API
     */
    @Transactional
    public void sign(String id) {
        SignRequest request = repository.find(id);
        notNull(request, () -> new MissingObject(SignRequest.class, id));
        eq(request.getState(), SignRequestState.NEW, () -> new ForbiddenObject(request, ForbiddenObject.ErrorCode.FORBIDDEN));

        request.setState(SignRequestState.SIGNED);
        repository.update(request);

        this.eventPublisher.publishEvent(new SignCompletedEvent(this, request));
    }

    /**
     * fixme: call from custom API
     */
    public List<SignRequest> listRequestsToSign(String userId) {
        return repository.listRequestsToSign(userId);
    }
}
