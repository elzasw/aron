package cz.aron.core.misc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lukas Jane (inQool) 25.11.2020.
 */
@RestController
@RequestMapping("/help")
public class HelpApi {
    @Value("${help-url}")
    private String helpUrl;

    @GetMapping
    public String getHelpUrl() {
        return helpUrl;
    }
}
