package cz.aron.core.image.tile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class TilesManager {

    private static final int DIR_NAME_LENGTH = 2;

    @Value("${tile.folder}")
    private String tileFolder;

    @Value("${tile.format}")
    private String tileFormat;

    @Value("${tile.level:2}")
    private int hierarchicalLevel;

    public Path getDescriptor(String id) {
        return getPath(id).resolve("image.dzi");
    }

    public Path getTileImage(String id, int level, int row, int column) {
        return getPath(id).resolve("image_files").resolve(""+level).resolve(""+ column + "_" + row + "." + tileFormat);
    }

    public Path getTilesPath(String id) {
        return getPath(id);
    }

    private Path getPath(String id) {
        String[] path = new String[hierarchicalLevel + 1];
        path[hierarchicalLevel] = id;

        String uuid = id.replaceAll("-", "");
        for (int i = 0; i < hierarchicalLevel; i++) {
            path[i] = uuid.substring(i * DIR_NAME_LENGTH, i * DIR_NAME_LENGTH + DIR_NAME_LENGTH);
        }

        return Paths.get(tileFolder, path);
    }

}