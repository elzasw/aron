package cz.aron.core.image;

import cz.aron.core.image.tile.ImageTiler;
import cz.aron.core.image.tile.TiledImage;
import cz.aron.core.image.tile.write.ImageWriter;
import cz.inqool.eas.common.storage.file.FileManager;
import cz.inqool.eas.common.storage.file.OpenedFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

@Service
@Slf4j
public class ImageProcessor {
    @Value("${tile.folder}")
    private String outputFolder;

    @Inject private ImageTiler tiler;
    @Inject private ImageWriter imageWriter;
    @Inject private FileManager fileManager;

    public void process(String fileId) throws IOException {
        OpenedFile openedFile = fileManager.open(fileId);
        cz.inqool.eas.common.storage.file.File file = openedFile.getDescriptor();
        InputStream is = openedFile.getStream();

        TiledImage image = tiler.tileJpeg(is);
        imageWriter.output(image, Paths.get(outputFolder).resolve(fileId));
    }
}
