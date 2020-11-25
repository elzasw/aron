package cz.aron.core.image.tile;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Lukas Jane (inQool) 24.11.2020.
 */
public interface ImageTiler {
    TiledImage tileJpeg(InputStream inputStream) throws IOException;
}
