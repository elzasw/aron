package cz.aron.core.image.tile.processing;//package cz.aron.core.image.tile.write;
//
//import cz.aron.core.image.tile.processing.Tile;
//import cz.aron.core.image.tile.processing.TiledImage;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import javax.imageio.ImageIO;
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBException;
//import javax.xml.bind.Marshaller;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//@Service
//@Slf4j
//public class ImageWriter {
//
//    public void output(final TiledImage image, final Path folder) throws IOException {
//        if(!Files.exists(folder)) {
//            Files.createDirectories(folder);
//        }
//        createDescriptorFile(folder, image);
//
//        final Path filesSubfolder = folder.resolve("image_files");
//        if(!Files.exists(filesSubfolder)) {
//            Files.createDirectories(filesSubfolder);
//        }
//        for (Tile tile : image.getTiles()) {
//            createTile(filesSubfolder, tile, "jpg");
//        }
//    }
//
//    private void createDescriptorFile(Path folder, TiledImage image) {
//        final Path descriptor = folder.resolve("image.dzi");
//        try {
//            JAXBContext context = JAXBContext.newInstance(TiledImage.class);
//            Marshaller marshaller = context.createMarshaller();
//            try (OutputStream os = Files.newOutputStream(descriptor)) {
//                marshaller.marshal(image, os);
//            }
//        } catch (JAXBException | IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void createTile(Path filesSubfolder, Tile tile, String format) throws IOException {
//        final Path levelFolder = filesSubfolder.resolve(String.valueOf(tile.getLevel()));
//        if(!Files.exists(levelFolder)) {
//            Files.createDirectories(levelFolder);
//        }
//        final String tileName = tile.getColumn() + "_" + tile.getRow() + "." + format;
//        try (OutputStream os = Files.newOutputStream(levelFolder.resolve(tileName))) {
//            ImageIO.write(tile.getData(), format, os);
//        }
//    }
//}
