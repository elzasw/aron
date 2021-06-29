package cz.inqool.eas.common.pdfa.mock;

import com.google.common.io.ByteStreams;
import cz.inqool.eas.common.pdfa.PdfaConverter;
import cz.inqool.eas.common.storage.file.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MockPdfaConverter extends PdfaConverter {
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void convertHook(File descriptor, InputStream input, OutputStream output) throws IOException {
        ByteStreams.copy(input, output);
    }
}
