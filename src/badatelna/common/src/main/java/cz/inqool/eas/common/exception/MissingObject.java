package cz.inqool.eas.common.exception;

import cz.inqool.eas.common.domain.Domain;
import lombok.Getter;

@Getter
public class MissingObject extends GeneralException implements CodedException {

    private final Class<?> clazz;
    private String objectId;
    private Enum<? extends ExceptionCodeEnum<?>> errorCode;


    public MissingObject(Class<?> clazz, String objectId) {
        this.clazz = clazz;
        this.objectId = objectId;
    }

    public MissingObject(Class<?> clazz, String objectId, Enum<? extends ExceptionCodeEnum<?>> errorCode) {
        this.clazz = clazz;
        this.objectId = objectId;
        this.errorCode = errorCode;
    }

    public MissingObject(Object object) {
        this(object, null);
    }

    public MissingObject(Object object, Enum<? extends ExceptionCodeEnum<?>> errorCode) {
        this.clazz = object.getClass();
        if (object instanceof Domain) {
            this.objectId = ((Domain<?>) object).getId();
        }
        this.errorCode = errorCode;
    }


    @Override
    public String getMessage() {
        return this.toString();
    }

    @Override
    public String toString() {
        return "MissingObject{" +
                "class=" + clazz +
                ", objectId='" + objectId + '\'' +
                ", errorCode=" + errorCode +
                '}';
    }


    public enum ErrorCode implements ExceptionCodeEnum<ErrorCode> {
        ELECTRONIC_ARCHIVAL_AID_WAS_REMOVED,
        FILE_WAS_REMOVED,
        NO_MATCHING_ITEM
    }
}
