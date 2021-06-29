import React from 'react';
import { useIntl } from 'react-intl';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence/authored-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { RecordsFields } from './records-fields';
import { useColumns } from './records-columns';

import { Record } from '../ess-types';
import { stubFalse } from 'lodash';

export function essRecordsFactory(url: string, reportTag: string | null) {
  return function Records() {
    const intl = useIntl();

    const evidence = useAuthoredEvidence<Record>({
      identifier: 'ESS_RECORDS',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'ESS_RECORDS_TABLE_TITLE',
          defaultMessage: 'Spisy',
        }),
        reportTag,
      },
      detailProps: {
        FieldsComponent: RecordsFields,
        toolbarProps: {
          showButton: stubFalse,
        },
      },
    });

    return <Evidence {...evidence} />;
  };
}
