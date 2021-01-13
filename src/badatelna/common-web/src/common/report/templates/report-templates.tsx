import React, { useContext } from 'react';
import { FormattedMessage } from 'react-intl';
import { v4 as uuidv4 } from 'uuid';
import {
  ReportTemplate,
  ListSource,
  DictionaryAutocomplete,
  ReportProvider,
} from 'common/common-types';
import { FilesProvider } from 'common/files/files-provider';
import { Evidence } from 'composite/evidence/evidence';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { ReportContext } from '../report-context';
import { useValidationSchema } from './report-templates-schema';
import { reportTemplatesFieldsFactory } from './report-templates-fields';
import { useColumns } from './report-templates-colums';
import { useListSource } from 'utils/list-source-hook';

export function reporTemplatesFactory(
  url: string,
  providersUrl: string,
  useReportTags: () => ListSource<DictionaryAutocomplete>,
  reportTag: string
) {
  function useReportProviders() {
    return useListSource<ReportProvider>({ url: providersUrl });
  }

  return function ReportTemplates() {
    const { url: fileUrl } = useContext(ReportContext);
    const validationSchema = useValidationSchema();
    const columns = useColumns(useReportProviders);

    const evidence = useDictionaryEvidence<ReportTemplate>({
      identifier: 'REPORT_TEMPLATES',
      apiProps: {
        url,
      },
      tableProps: {
        columns,
        tableName: (
          <FormattedMessage
            id="EAS_REPORT_TEMPLATES_TABLE_TITLE"
            defaultMessage="Tiskové šablony"
          />
        ),
        reportTag,
      },
      detailProps: {
        FieldsComponent: reportTemplatesFieldsFactory(
          useReportTags,
          useReportProviders
        ),
        validationSchema,
        initNewItem: () => ({
          id: uuidv4(),
          name: '',
          params: '{}',
        }),
      },
    });

    return (
      <FilesProvider url={fileUrl}>
        <Evidence {...evidence} />
      </FilesProvider>
    );
  };
}
