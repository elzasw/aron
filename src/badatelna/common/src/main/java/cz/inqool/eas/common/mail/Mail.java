package cz.inqool.eas.common.mail;

import cz.inqool.eas.common.authored.store.AuthoredObject;
import cz.inqool.eas.common.domain.DomainViews;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import javax.persistence.*;

/**
 * Enqueued or sent mail.
 *
 * TODO: add support for attachments.
 */
@DomainViews
@Setter
@Getter
@Entity
@Table(name = "eas_mail")
public class Mail extends AuthoredObject<Mail> {
    /**
     * Mail subject.
     */
    @Nationalized
    protected String subject;

    @Nationalized
    protected String content;

    /**
     * Stav odeslání.
     */
    @Enumerated(EnumType.STRING)
    protected MailState state;

    /**
     * Identifikátor.
     */
    protected String identifier;

    /**
     * MIME type.
     */
    protected String contentType;

    @Column(name="\"to\"")
    protected String to;

    protected boolean sent;

    protected String error;
}
