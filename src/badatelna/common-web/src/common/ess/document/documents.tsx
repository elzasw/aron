import React from 'react';
import { useIntl } from 'react-intl';
import { stubFalse } from 'lodash';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence/authored-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { DocumentsFields } from './documents-fields';
import { useColumns } from './documents-columns';
import { Document } from '../ess-types';

export function essDocumentsFactory(url: string, reportTag: string | null) {
  return function Documents() {
    const intl = useIntl();

    const evidence = useAuthoredEvidence<Document>({
      identifier: 'ESS_DOCUMENTS',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'ESS_DOCUMENTS_TABLE_TITLE',
          defaultMessage: 'Dokumenty',
        }),
        reportTag,
      },
      detailProps: {
        FieldsComponent: DocumentsFields,
        toolbarProps: {
          showButton: stubFalse,
        },
      },
    });

    return <Evidence {...evidence} />;
  };
}
