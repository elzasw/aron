package cz.inqool.eas.common.ws.soap.logger.message;

import cz.inqool.eas.common.authored.AuthoredRepository;
import cz.inqool.eas.common.authored.index.AuthoredIndex;
import cz.inqool.eas.common.authored.store.AuthoredStore;

public class SoapMessageRepository extends AuthoredRepository<
        SoapMessage,
        SoapMessage,
        SoapMessageIndexedObject,
        AuthoredStore<SoapMessage, SoapMessage, QSoapMessage>,
        AuthoredIndex<SoapMessage, SoapMessage, SoapMessageIndexedObject>> {
}
