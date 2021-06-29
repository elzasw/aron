import React from 'react';
import { useIntl } from 'react-intl';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { DocumentType } from '../../ess-types';

export function essDocumentTypesFactory(url: string, reportTag: string | null) {
  return function DocumentTypes() {
    const intl = useIntl();

    const evidence = useDictionaryEvidence<DocumentType>({
      identifier: 'ESS_DOCUMENT_TYPES',
      apiProps: {
        url,
      },
      tableProps: {
        tableName: intl.formatMessage({
          id: 'ESS_RECORDS_TABLE_TITLE',
          defaultMessage: 'Typy dokumentů',
        }),
        reportTag,
      },
      detailProps: {},
    });

    return <Evidence {...evidence} />;
  };
}
