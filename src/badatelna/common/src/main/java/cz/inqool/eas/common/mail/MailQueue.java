package cz.inqool.eas.common.mail;

import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.List;

public class MailQueue {
    private MailStore store;

    @Transactional
    public Mail queue(String to, String subject, String content, boolean isHtml) {
        Mail mail = new Mail();
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setContentType(isHtml ? "text/html" : "text/plain");
        mail.setContent(content);

        return store.create(mail);
    }

    @Transactional
    public List<Mail> getWaiting() {
        return store.getWaiting();
    }

    @Transactional
    public void updateMail(Mail mail) {
        store.update(mail);
    }

    @Autowired
    public void setStore(MailStore store) {
        this.store = store;
    }
}
