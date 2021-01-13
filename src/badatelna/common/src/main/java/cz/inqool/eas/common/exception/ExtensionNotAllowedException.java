package cz.inqool.eas.common.exception;

import lombok.Getter;

/**
 * Class for throwing exceptions when there was a file with not allowed extension uploaded.
 */
@Getter
public class ExtensionNotAllowedException extends GeneralException  {

    private final String name;

    public ExtensionNotAllowedException(String name) {
        this.name = name;

    }

    @Override
    public String toString() {
        return "ExtensionNotAllowedException{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public String getMessage() {
        return toString();
    }
}
