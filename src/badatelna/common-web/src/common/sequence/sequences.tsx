import React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { SequencesFields } from './sequences-fields';
import { useColumns } from './sequences-columns';
import { useValidationSchema } from './sequences-schema';
import { Sequence } from './sequences-types';

export function sequencesFactory(url: string, reportTag: string) {
  return function Sequences() {
    const validationSchema = useValidationSchema();

    const evidence = useDictionaryEvidence<Sequence>({
      identifier: 'SEQUENCES',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: (
          <FormattedMessage
            id="EAS_SEQUENCES_TABLE_TITLE"
            defaultMessage="Číselné řady"
          />
        ),
        reportTag,
      },
      detailProps: {
        FieldsComponent: SequencesFields,
        validationSchema,
      },
    });

    return <Evidence {...evidence} />;
  };
}
