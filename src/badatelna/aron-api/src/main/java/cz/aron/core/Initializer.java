package cz.aron.core;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;;

/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@ComponentScan({"cz.inqool","cz.aron.core"})
@EntityScan({"cz.inqool.eas.common.storage.file","cz.aron.core"})
//@ConfigurationPropertiesScan("cz.inqool")
@OpenAPIDefinition(info = @Info(title = "ARON backend", description = "Main ARON backend service", version = "1"))
public class Initializer {
    public static void main(String[] args) {
        SpringApplication.run(Initializer.class, args);
    }
}
