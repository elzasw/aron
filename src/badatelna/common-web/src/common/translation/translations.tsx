import React from 'react';
import { useIntl } from 'react-intl';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { useValidationSchema } from './translations-schema';
import { Translation } from './translations-types';
import { useColumns } from './translations-columns';
import { TranslationsFields } from './translations-fields';

export function translationsFactory(url: string, reportTag: string) {
  return function Translations() {
    const intl = useIntl();
    const validationSchema = useValidationSchema();

    const evidence = useDictionaryEvidence<Translation>({
      identifier: 'TRANSLATIONS',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'EAS_TRANSLATIONS_TABLE_TITLE',
          defaultMessage: 'Překlady',
        }),
        reportTag,
      },
      detailProps: {
        FieldsComponent: TranslationsFields,
        validationSchema,
      },
    });

    return <Evidence {...evidence} />;
  };
}
