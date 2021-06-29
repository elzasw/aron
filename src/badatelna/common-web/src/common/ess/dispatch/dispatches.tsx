import React from 'react';
import { useIntl } from 'react-intl';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence/authored-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { DispatchesFields } from './dispatches-fields';
import { useColumns } from './dispatches-columns';
import { Dispatch } from '../ess-types';
import { stubFalse } from 'lodash';
import { DispatchToolbar } from './dispatches-detail-toolbar';

export function essDispatchesFactory(url: string, reportTag: string | null) {
  return function Dispatches() {
    const intl = useIntl();

    const evidence = useAuthoredEvidence<Dispatch>({
      identifier: 'ESS_DISPATCHES',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'ESS_DISPATCHES_TABLE_TITLE',
          defaultMessage: 'Vypravení',
        }),
        reportTag,
      },
      detailProps: {
        FieldsComponent: DispatchesFields,
        toolbarProps: {
          after: <DispatchToolbar url={url} />,
          showButton: stubFalse,
        },
      },
    });

    return <Evidence {...evidence} />;
  };
}
