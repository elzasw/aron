package cz.aron.core.files;

import cz.inqool.eas.common.storage.StorageConfiguration;
import cz.inqool.eas.common.storage.file.FileManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author Lukas Jane (inQool) 04.11.2020.
 */
@Configuration
public class FileStorageConfig extends StorageConfiguration {

    @Value("${files.storage}")
    private String fileStorage;

    @Override
    protected void configure(FileManager.FileManagerBuilder builder) {
        builder.directoryPath(fileStorage)
                .fileSizeLimit((long) 1024 * 1024 * 100)   // 100 MB
                .hierarchicalLevel(2);
    }

    @Override
    protected String fileUrl() {
        return "/file";
    }
}
