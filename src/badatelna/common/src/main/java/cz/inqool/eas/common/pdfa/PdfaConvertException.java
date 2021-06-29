package cz.inqool.eas.common.pdfa;


import cz.inqool.eas.common.exception.GeneralException;

public class PdfaConvertException extends GeneralException {

    public PdfaConvertException(String message) {
        super(message);
    }

    public PdfaConvertException(String message, Throwable cause) {
        super(message, cause);
    }

}
