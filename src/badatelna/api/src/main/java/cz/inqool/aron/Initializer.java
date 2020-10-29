package cz.inqool.aron;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@SpringBootApplication
@EnableAsync
public class Initializer {
    public static void main(String[] args) {
        SpringApplication.run(Initializer.class, args);
    }
}
