import React from 'react';
import { useIntl } from 'react-intl';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { ActionsFields } from './actions-fields';
import { useColumns } from './actions-columns';
import { useValidationSchema } from './actions-schema';
import { Action } from './actions-types';
import { TableSort } from 'composite/table/table-types';

export function actionsFactory(
  url: string,
  reportTag: string | null,
  defaultSorts?: TableSort[]
) {
  return function Actions() {
    const intl = useIntl();
    const validationSchema = useValidationSchema();

    const evidence = useDictionaryEvidence<Action>({
      identifier: 'ACTIONS',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'EAS_ACTIONS_TABLE_TITLE',
          defaultMessage: 'Skripty',
        }),
        reportTag,
        defaultSorts,
      },
      detailProps: {
        FieldsComponent: ActionsFields,
        validationSchema,
      },
    });

    return <Evidence {...evidence} />;
  };
}
