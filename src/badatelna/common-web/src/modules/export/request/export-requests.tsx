import React, { useContext } from 'react';
import { useIntl } from 'react-intl';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence/authored-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { useColumns } from './export-requests-colums';
import { ExportRequest } from '../export-types';
import { ExportRequestsFields } from './export-requests-fields';
import { ExportContext } from '../export-context';

export function exportRequestsFactory({ exportTag }: { exportTag?: string }) {
  return function ExportRequests() {
    const intl = useIntl();
    const columns = useColumns();
    const { url } = useContext(ExportContext);

    const evidence = useAuthoredEvidence<ExportRequest>({
      identifier: 'EXPORT_REQUESTS',
      apiProps: {
        url: `${url}/requests`,
      },
      tableProps: {
        columns,
        tableName: intl.formatMessage({
          id: 'EAS_EXPORT_REQUESTS_TABLE_TITLE',
          defaultMessage: 'Tisková fronta',
        }),
        reportTag: exportTag,
      },
      detailProps: {
        FieldsComponent: ExportRequestsFields,
      },
    });

    return <Evidence {...evidence} />;
  };
}

exportRequestsFactory.useColumns = useColumns;
exportRequestsFactory.Fields = ExportRequestsFields;
