import React, { useContext } from 'react';
import { useIntl } from 'react-intl';
import { v4 as uuidv4 } from 'uuid';
import { Evidence } from 'composite/evidence/evidence';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { TableSort } from 'composite/table/table-types';
import { ExportContext } from '../export-context';
import { useColumns } from './export-templates-colums';
import { useValidationSchema } from './export-templates-schema';
import { ExportTemplate } from '../export-types';
import { ExportTemplatesFields } from './export-templates-fields';

export function exportTemplatesFactory({
  exportTag,
  defaultSorts,
}: {
  exportTag: string;
  defaultSorts?: TableSort[];
}) {
  return function ExportTemplates() {
    const intl = useIntl();
    const { url } = useContext(ExportContext);
    const validationSchema = useValidationSchema();
    const columns = useColumns();

    const evidence = useDictionaryEvidence<ExportTemplate>({
      identifier: 'EXPORT_TEMPLATES',
      apiProps: {
        url: `${url}/templates`,
      },
      tableProps: {
        columns,
        tableName: intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_TABLE_TITLE',
          defaultMessage: 'Tiskové šablony',
        }),
        reportTag: exportTag,
        defaultSorts,
      },
      detailProps: {
        FieldsComponent: ExportTemplatesFields,
        validationSchema,
        initNewItem: () => ({
          id: uuidv4(),
          name: '',
          params: '{}',
        }),
      },
    });

    return <Evidence {...evidence} />;
  };
}

exportTemplatesFactory.useColumns = useColumns;
exportTemplatesFactory.useValidationSchema = useValidationSchema;
exportTemplatesFactory.Fields = ExportTemplatesFields;
