import React from 'react';
import { useIntl } from 'react-intl';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { ShreddingModesFields } from './shredding-modes-fields';
import { useColumns } from './shredding-modes-columns';
import { ShreddingMode } from 'common/ess/ess-types';

export function essShreddingModesFactory(
  url: string,
  reportTag: string | null
) {
  return function ShreddingModes() {
    const intl = useIntl();

    const evidence = useDictionaryEvidence<ShreddingMode>({
      identifier: 'ESS_SHREDDING_MODES',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'ESS_SHREDDING_MODES_TABLE_TITLE',
          defaultMessage: 'Skartačné režimy',
        }),
        reportTag,
      },
      detailProps: {
        FieldsComponent: ShreddingModesFields,
      },
    });

    return <Evidence {...evidence} />;
  };
}
