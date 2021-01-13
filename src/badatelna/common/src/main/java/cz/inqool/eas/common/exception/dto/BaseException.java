package cz.inqool.eas.common.exception.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.inqool.eas.common.exception.CodedException;
import cz.inqool.eas.common.exception.CodedException.ExceptionCodeEnum;
import cz.inqool.eas.common.exception.DetailedException;
import cz.inqool.eas.common.exception.GeneralException;

import java.time.Instant;

import static cz.inqool.eas.common.exception.ExceptionUtils.getLastCause;
import static cz.inqool.eas.common.utils.JsonUtils.toJsonString;

/**
 * Abstract class serving as a base for exception handling objects.
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public abstract class BaseException {

    @JsonIgnore
    protected Throwable throwable;

    protected String timestamp;
    protected String message;
    protected Enum<? extends ExceptionCodeEnum<?>> code;
    protected Object details;


    protected BaseException(Throwable exception) {
        this(exception, exception.getMessage());
    }

    protected BaseException(Throwable exception, String message) {
        this.throwable = exception;
        this.timestamp = Instant.now().toString();
        this.message = message;
        if (exception instanceof CodedException) {
            this.code = ((CodedException) exception).getErrorCode();
        }
        if (exception instanceof DetailedException) {
            this.details = ((DetailedException) exception).getDetails();
        }
    }


    protected ObfuscatedException toObfuscatedException() {
        return new ObfuscatedException(getException().getName(), message, null);
    }

    public Class<?> getException() {
        return throwable.getClass();
    }

    public String getCause() {
        Throwable cause = getLastCause(throwable);
        if (cause != throwable) {
            if (cause instanceof GeneralException) {
                return cause.toString();
            } else {
                return cause.getClass().getName() + ": " + cause.getMessage();
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return toJsonString(this, true);
    }
}
