package cz.inqool.eas.common.ws.soap.logger.message;

import cz.inqool.eas.common.authored.AuthoredService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SoapMessageService extends AuthoredService<
        SoapMessage,
        SoapMessageDetail,
        SoapMessageList,
        SoapMessageCreate,
        SoapMessageUpdate,
        SoapMessageRepository
        > {
}
