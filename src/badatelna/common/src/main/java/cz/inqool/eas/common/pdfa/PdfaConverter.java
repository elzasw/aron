package cz.inqool.eas.common.pdfa;

import cz.inqool.eas.common.storage.file.File;
import cz.inqool.eas.common.storage.file.FileManager;
import cz.inqool.eas.common.storage.file.OpenedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.io.*;

@Slf4j
public abstract class PdfaConverter {
    private FileManager fileManager;

    @Transactional
    public File convert(File inputFile) {
        OpenedFile openedFile = fileManager.open(inputFile.getId());
        File descriptor = openedFile.getDescriptor();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (InputStream input = openedFile.getStream()) {
            convertHook(descriptor, input, output);
        } catch (IOException e) {
            throw new PdfaConvertException("Failed to convert to PDFA.", e);
        }

        return fileManager.store(
                descriptor.getName(),
                output.size(),
                "application/pdf",
                new ByteArrayInputStream(output.toByteArray())
        );
    }

    public abstract void convertHook(File descriptor, InputStream input, OutputStream output) throws IOException;

    @Autowired
    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }
}
