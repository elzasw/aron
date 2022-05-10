package cz.aron.core.image.tile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

/**
 * @author Lukas Jane (inQool) 28.08.2018.
 */
@RestController
@RequestMapping("/tile")
public class TileRS {

    @Autowired
    private TilesManager tilesManager;

    @RequestMapping(value = "/{id}/image.dzi", method = RequestMethod.GET, produces = MediaType.TEXT_XML_VALUE)
    public FileSystemResource getDescriptor(@PathVariable("id") String id) {
        return new FileSystemResource(tilesManager.getDescriptor(id));
    }

    @RequestMapping(value = "/{id}/image_files/{level}/{column}_{row}.jpg", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    public FileSystemResource getTileImage(@PathVariable("id") String id,
                             @PathVariable("level") int level,
                             @PathVariable("row") int row,
                             @PathVariable("column") int column) {
        return new FileSystemResource(tilesManager.getTileImage(id, level, row, column));
    }
}
