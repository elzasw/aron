package cz.inqool.eas.common.ws.soap.logger.message;

import cz.inqool.eas.common.authored.store.AuthoredObject;
import cz.inqool.eas.common.domain.DomainViews;
import cz.inqool.entityviews.Viewable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Soap message is used for logging request/response of web service
 */
@Viewable
@DomainViews
@Getter
@Setter
@Entity
@Table(name = "eas_soap_message")
@BatchSize(size = 100)
public class SoapMessage extends AuthoredObject<SoapMessage> {
    protected String service;

    protected String request;

    protected String response;
}

