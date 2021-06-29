import React, { useContext } from 'react';
import { useIntl } from 'react-intl';
import { Evidence } from 'composite/evidence/evidence';
import { MessagesFields } from './messages-fields';
import { useColumns } from './messages-columns';
import { SoapMessage } from './messages-types';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence/authored-evidence';
import { AdminContext } from 'common/admin/admin-context';

export function SoapMessages() {
  const intl = useIntl();

  const { soapMessagesUrl } = useContext(AdminContext);

  const evidence = useAuthoredEvidence<SoapMessage>({
    identifier: 'SOAP_MESSAGES',
    apiProps: {
      url: soapMessagesUrl,
    },
    tableProps: {
      columns: useColumns(),
      tableName: intl.formatMessage({
        id: 'EAS_SOAP_LOGGER_MESSAGES_TABLE_TITLE',
        defaultMessage: 'SOAP komunikace',
      }),
    },
    detailProps: {
      FieldsComponent: MessagesFields,
    },
  });

  return <Evidence {...evidence} />;
}
