import React, { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import { ReportRequest } from 'common/common-types';
import { FilesProvider } from 'common/files/files-provider';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { ReportRequestsFields } from './report-requests-fields';
import { useColumns } from './report-requests-colums';
import { ReportContext } from '../report-context';

export function reportRequestsFactory(url: string, reportTag: string) {
  return function AdminReportRequests() {
    const { url: fileUrl } = useContext(ReportContext);
    const columns = useColumns();

    const evidence = useAuthoredEvidence<ReportRequest>({
      identifier: 'REPORT_REQUESTS',
      apiProps: {
        url,
      },
      tableProps: {
        columns,
        tableName: (
          <FormattedMessage
            id="EAS_REPORT_REQUESTS_TABLE_TITLE"
            defaultMessage="Tisková fronta"
          />
        ),
        reportTag,
      },
      detailProps: {
        FieldsComponent: ReportRequestsFields,
      },
    });

    return (
      <FilesProvider url={fileUrl}>
        <Evidence {...evidence} />
      </FilesProvider>
    );
  };
}
