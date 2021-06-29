import React from 'react';
import { useIntl } from 'react-intl';
import { stubFalse } from 'lodash';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence/authored-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { AlogFields } from './alog-fields';
import { useColumns } from './alog-columns';
import { AlogEvent } from './alog-types';

export function auditLogFactory({
  url,
  reportTag,
}: {
  url: string;
  reportTag?: string;
}) {
  return function Auditlog() {
    const intl = useIntl();

    const evidence = useAuthoredEvidence<AlogEvent>({
      identifier: 'ALOG',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'EAS_ALOG_TABLE_TITLE',
          defaultMessage: 'Auditní log',
        }),
        reportTag,
      },
      detailProps: {
        toolbarProps: {
          showButton: stubFalse,
        },
        FieldsComponent: AlogFields,
      },
    });

    return <Evidence {...evidence} />;
  };
}

auditLogFactory.useColumns = useColumns;
auditLogFactory.Fields = AlogFields;
