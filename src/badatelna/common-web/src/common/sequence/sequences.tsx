import React from 'react';
import { useIntl } from 'react-intl';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { SequencesFields } from './sequences-fields';
import { useColumns } from './sequences-columns';
import { useValidationSchema } from './sequences-schema';
import { Sequence } from './sequences-types';
import { TableSort } from 'composite/table/table-types';

export function sequencesFactory(
  url: string,
  reportTag: string | null,
  defaultSorts?: TableSort[]
) {
  return function Sequences() {
    const intl = useIntl();
    const validationSchema = useValidationSchema();

    const evidence = useDictionaryEvidence<Sequence>({
      identifier: 'SEQUENCES',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'EAS_SEQUENCES_TABLE_TITLE',
          defaultMessage: 'Číselné řady',
        }),
        reportTag,
        defaultSorts,
      },
      detailProps: {
        FieldsComponent: SequencesFields,
        validationSchema,
      },
    });

    return <Evidence {...evidence} />;
  };
}
