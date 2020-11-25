package cz.inqool.eas.common.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * TODO: make the timer configurable.
 */
@Slf4j
public class MailScheduler {
    private MailQueue queue;

    private MailSender sender;

    @Scheduled(fixedDelay = 5000)
    public void run() {
        List<Mail> waiting = queue.getWaiting();

        if (waiting.size() == 0) {
            // log.trace("No mail is waiting in queue.");
            return;
        }

        log.debug("Sending {} mails.", waiting.size());

        waiting.forEach(mail -> {
            try {
                sender.send(mail);
                mail.setSent(true);
            } catch (Exception e) {
                mail.setError(e.getMessage());
            }

            queue.updateMail(mail);
        });
    }

    @Autowired
    public void setQueue(MailQueue queue) {
        this.queue = queue;
    }

    @Autowired
    public void setSender(MailSender sender) {
        this.sender = sender;
    }
}
