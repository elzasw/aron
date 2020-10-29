package cz.inqool.aron;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * @author Lukas Jane (inQool) 26.10.2020.
 */
@Component
@Slf4j
public class PostInitializer implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Running PostInitializer");
        try {


        } catch (Exception e) {
            log.error("boo boo", e);
        }
        log.info("PostInitializer complete");
    }
}
