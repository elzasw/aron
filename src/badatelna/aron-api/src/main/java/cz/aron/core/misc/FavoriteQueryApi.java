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
@RequestMapping("/favoriteQuery")
public class FavoriteQueryApi {
    @Value("${webResources.favoriteQueries}")
    private String favoriteQueriesFile;

    private String favoriteQueriesData;

    @GetMapping(produces = "application/x-yaml")
    public String getFavoriteQueries() throws IOException {
        if (favoriteQueriesData == null) {
            favoriteQueriesData = Files.readString(Paths.get(favoriteQueriesFile), StandardCharsets.UTF_8);
        }
        return favoriteQueriesData;
    }
}
