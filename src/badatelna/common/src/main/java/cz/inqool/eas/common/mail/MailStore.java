package cz.inqool.eas.common.mail;

import cz.inqool.eas.common.authored.store.AuthoredStore;

import java.util.List;


public class MailStore extends AuthoredStore<Mail, Mail, QMail> {
    public MailStore() {
        super(Mail.class);
    }

    public List<Mail> getWaiting() {
        QMail model = QMail.mail;

        List<Mail> mails = query().
                select(model).
                from(model).
                where(model.deleted.isNull()).
                where(model.state.eq(MailState.QUEUED)).
                fetch();

        detachAll();

        return mails;
    }
}
