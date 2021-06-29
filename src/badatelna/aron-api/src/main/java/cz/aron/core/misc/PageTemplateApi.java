package cz.aron.core.misc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Lukas Jane (inQool) 25.11.2020.
 */
@RestController
@RequestMapping("/pageTemplate")
public class PageTemplateApi {
    @Value("${webResources.pageTemplate}")
    private String pageTemplateFile;
    @Value("${webResources.logo}")
    private String logo;
    @Value("${webResources.topImage}")
    private String topImage;

    private String pageTemplateData;
    private String iconData;
    private byte[] topImageData;

    @GetMapping(produces = "application/x-yaml")
    public String getPageTemplate() throws IOException {
        if (pageTemplateData == null) {
            pageTemplateData = Files.readString(Paths.get(pageTemplateFile), StandardCharsets.UTF_8);
        }
        return pageTemplateData;
    }

    @GetMapping(path = "/logo", produces = "image/svg+xml")
    public String getLogo() throws IOException {
        if (iconData == null) {
            iconData = Files.readString(Paths.get(this.logo), StandardCharsets.UTF_8);
        }
        return iconData;
    }

    @GetMapping(path = "/topImage", produces = "image/png")
    public byte[] getTopImage() throws IOException {
        if (topImageData == null) {
            topImageData = Files.readAllBytes(Paths.get(topImage));
        }
        return topImageData;
    }
}
