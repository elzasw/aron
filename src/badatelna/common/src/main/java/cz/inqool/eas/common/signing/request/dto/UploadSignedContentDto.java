package cz.inqool.eas.common.signing.request.dto;

import cz.inqool.eas.common.signing.request.SignContent;
import cz.inqool.eas.common.storage.file.File;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadSignedContentDto {
    private SignContent content;
    private File signed;
}
