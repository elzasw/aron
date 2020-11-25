package cz.aron.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Lukas Jane (inQool) 02.11.2020.
 */
@RestController
@RequestMapping("/debug")
@Slf4j
public class DebugRS {

    @GetMapping(value = "/test")
    public String test() {
        log.info("Test called.");

        log.info("Test finished.");
        return "done";
    }
}

