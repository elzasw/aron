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
@RequestMapping("/news")
public class NewsApi {
    @Value("${webResources.news}")
    private String newsFile;

    @GetMapping(produces = "application/x-yaml")
    public String getNews() throws IOException {
        return Files.readString(Paths.get(newsFile), StandardCharsets.UTF_8);
    }
}
