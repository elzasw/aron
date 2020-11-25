package cz.inqool.eas.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
public abstract class MailConfiguration {

    /**
     * Constructs {@link MailStore} bean.
     */
    @Bean
    public MailStore mailStore() {
        return new MailStore();
    }

    @Bean
    public MailQueue mailService() {
        return new MailQueue();
    }

    @Bean
    public MailSender mailSender() {
        return new MailSender();
    }

    @Bean
    public MailScheduler mailScheduler() {
        return new MailScheduler();
    }
}
