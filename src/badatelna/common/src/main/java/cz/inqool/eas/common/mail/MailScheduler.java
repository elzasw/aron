package cz.inqool.eas.common.mail;

import cz.inqool.eas.common.mail.event.MailErrorEvent;
import cz.inqool.eas.common.mail.event.MailSentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * TODO: make the timer configurable.
 */
@Slf4j
public class MailScheduler {
    private MailQueue queue;

    private MailSender sender;

    protected ApplicationEventPublisher eventPublisher;

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
                mail.setState(MailState.SENT);
                queue.updateMail(mail);

                eventPublisher.publishEvent(new MailSentEvent(this, mail));
            } catch (Exception e) {
                mail.setError(e.getMessage());
                mail.setState(MailState.ERROR);
                queue.updateMail(mail);

                eventPublisher.publishEvent(new MailErrorEvent(this, mail));
            }
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

    @Autowired
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
