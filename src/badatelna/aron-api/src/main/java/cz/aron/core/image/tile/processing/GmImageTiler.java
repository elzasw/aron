//package cz.aron.core.image.tile;
//
//import cz.inqool.uas.opsa.image.gm.ConvertTempCmd;
//import lombok.extern.slf4j.Slf4j;
//import org.im4java.core.*;
//import org.im4java.process.ProcessStarter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@Slf4j
//public class GmImageTiler implements ImageTiler {
//    @Value("${tile.size}")
//    private Integer tileSize;
//
//    @Value("${tile.format}")
//    private String tileFormat;
//
//    @Value("${tile.gm}")
//    private String gmFolder;
//
//    @Value("${tile.tmp}")
//    private String tempFolder;
//
//    @PostConstruct
//    private void postConstruct() {
//        System.setProperty("im4java.useGM", "true");
//        ProcessStarter.setGlobalSearchPath(gmFolder);
//    }
//
//    @Override
//    public TiledImage tileJpeg(File file) throws IOException {
//        log.debug("Image tiling started.");
//        try {
//            // convert to MPC
//            log.debug("Creating MPC from image.");
//            File image = getMpc(file);
//
//            log.debug("Retrieving image information.");
//            Info imageInfo = new Info(image.getAbsolutePath(), true);
//
//            int imageWidth = imageInfo.getImageWidth();
//            int imageHeight = imageInfo.getImageHeight();
//
//            int imageMax = Math.max(imageWidth, imageHeight);
//            int levels = (int) Math.ceil(Math.log(imageMax) / Math.log(2));
//
//            log.debug("Width: {}, Height: {}, Levels: {}", imageWidth, imageHeight, levels);
//            double width = imageWidth;
//            double height = imageHeight;
//
//            List<Tile> tiles = new ArrayList<>();
//            for (int level = levels; level >= 0; level--) {
//                int nCols = (int) Math.ceil(width / tileSize);
//                int nRows = (int) Math.ceil(height / tileSize);
//                log.debug("Level: {}, Width: {}, Height: {}, Columns: {}, Rows: {}", level, width, height, nCols, nRows);
//                tiles.addAll(getTiles(image, nCols, nRows, level));
//                // Scale down image for next level
//                width = Math.ceil(width / 2);
//                height = Math.ceil(height / 2);
//                File oldImage = image;
//                image = resizeImage(image, width, height);
//                deleteMpc(oldImage);
//            }
//            deleteMpc(image);
//            return new TiledImage(tileFormat, 0, tileSize, imageWidth, imageHeight, tiles);
//        } catch (InfoException ex) {
//            throw new IOException(ex);
//        }
//    }
//
//    private void deleteMpc(File file) {
//        if (file != null) {
//            String path = file.getAbsolutePath();
//            File cache = new File(path.substring(0, path.length() - ".mpc".length()) + ".cache");
//            file.delete();
//            cache.delete();
//        }
//    }
//
//    private File getMpc(File img) throws IOException {
//        File image = new File(tempFolder, UUID.randomUUID().toString() + ".mpc");
//        try {
//            Info imageInfo = new Info(img.getAbsolutePath(), true);
//            ConvertCmd cmd = new ConvertTempCmd(tempFolder);
//            IMOperation op = new IMOperation();
//            op.addImage(img.getAbsolutePath());
//            op.resize(imageInfo.getImageWidth(), imageInfo.getImageHeight());
//            op.addImage(image.getAbsolutePath());
//            cmd.run(op);
//            return image;
//        } catch (InterruptedException | IM4JavaException e) {
//            throw new IOException(e);
//        }
//    }
//
//    private List<Tile> getTiles(File img, int nCols, int nRows, int level) throws IOException {
//        List<Tile> tiles = new ArrayList<>();
//        File image = new File(tempFolder, UUID.randomUUID().toString() + "");
//        try {
//            ConvertCmd cmd = new ConvertTempCmd(tempFolder);
//            // crop whole image into tiles with one command
//            IMOperation op = new IMOperation();
//            op.crop(tileSize, tileSize);
//            op.addImage(img.getAbsolutePath());
//            op.p_adjoin();
//            op.addImage(image.getAbsolutePath() + "_%d."+tileFormat);
//            try {
//                cmd.run(op);
//            } catch (InterruptedException | IM4JavaException e) {
//                throw new IOException(e);
//            }
//            // construct files on top of created files
//            for (int row = 0; row < nRows; row++) {
//                for (int col = 0; col < nCols; col++) {
//                    int index = (row * nCols) + col;
//                    File data = new File(image.getAbsolutePath() + "_" + index + "." + tileFormat);
//
//                    tiles.add(new Tile(data, level, row, col));
//                }
//            }
//            return tiles;
//        } finally {
//            image.delete();
//        }
//    }
//
//    private File resizeImage(File img, double width, double height) throws IOException {
//        File image = new File(tempFolder, UUID.randomUUID().toString() + ".mpc");
//
//        ConvertCmd cmd = new ConvertTempCmd(tempFolder);
//
//        IMOperation op = new IMOperation();
//        op.addImage(img.getAbsolutePath());
//        op.resize((int)width,(int)height);
//        op.addImage(image.getAbsolutePath());
//        try {
//            cmd.run(op);
//        } catch (InterruptedException | IM4JavaException e) {
//            throw new IOException(e);
//        }
//        return image;
//    }
//}
