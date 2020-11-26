package cz.aron.core.image.tile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class JavaImageTiler implements ImageTiler {
    @Value("${tile.size}")
    private Integer tileSize;

    @Value("${tile.format}")
    private String tileFormat;

    @Value("${tile.tmp}")
    private String tempFolder;

    @Override
    public TiledImage tileJpeg(InputStream inputStream) throws IOException {
        log.debug("Image tiling started.");
        log.debug("Retrieving image information.");

        BufferedImage bimg = ImageIO.read(inputStream);
        int imageWidth = bimg.getWidth();
        int imageHeight = bimg.getHeight();

        int imageMax = Math.max(imageWidth, imageHeight);
        int levels = (int) Math.ceil(Math.log10(imageMax));

        log.debug("Width: {}, Height: {}, Levels: {}", imageWidth, imageHeight, levels);
        double width = imageWidth;
        double height = imageHeight;

        BufferedImage scaledImage = bimg;
        List<Tile> tiles = new ArrayList<>();
        for (int level = levels; level >= 0; level--) {
            int nCols = (int) Math.ceil(width / tileSize);
            int nRows = (int) Math.ceil(height / tileSize);
            log.debug("Level: {}, Width: {}, Height: {}, Columns: {}, Rows: {}", level, width, height, nCols, nRows);
            tiles.addAll(getTiles(scaledImage, nCols, nRows, level));
            // Scale down image for next level
            width = Math.ceil(width / 2);
            height = Math.ceil(height / 2);
            scaledImage = resizeImage(bimg, (int) width, (int) height);
        }
        return new TiledImage(tileFormat, 0, tileSize, imageWidth, imageHeight, tiles);
    }

    private List<Tile> getTiles(BufferedImage img, int nCols, int nRows, int level) {
        List<Tile> tiles = new ArrayList<>();
        for (int row = 0; row < nRows; row++) {
            for (int col = 0; col < nCols; col++) {
                BufferedImage croppedImg = cropImage(
                        img,
                        col * tileSize,
                        row * tileSize,
                        Math.min(tileSize, img.getWidth() - col * tileSize),
                        Math.min(tileSize, img.getHeight() - row * tileSize));
                tiles.add(new Tile(croppedImg, level, row, col));
            }
        }
        return tiles;
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

    private BufferedImage cropImage(BufferedImage originalImage, int startX, int startY, int width, int height) {
        BufferedImage subimageView = originalImage.getSubimage(startX, startY, width, height);
        BufferedImage croppedImage = new BufferedImage(subimageView.getWidth(), subimageView.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = croppedImage.createGraphics();
        g.drawImage(subimageView, 0, 0, null);
        g.dispose();
        return croppedImage;
    }
}
