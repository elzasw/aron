import React from 'react';
import { useIntl } from 'react-intl';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence/authored-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { ComponentsFields } from './components-fields';
import { useColumns } from './components-columns';
import { Component } from '../ess-types';
import { stubFalse } from 'lodash';

export function essComponentsFactory(url: string, reportTag: string | null) {
  return function Components() {
    const intl = useIntl();

    const evidence = useAuthoredEvidence<Component>({
      identifier: 'ESS_COMPONENTS',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'ESS_COMPONENTS_TABLE_TITLE',
          defaultMessage: 'Komponenty',
        }),
        reportTag,
      },
      detailProps: {
        FieldsComponent: ComponentsFields,
        toolbarProps: {
          showButton: stubFalse,
        },
      },
    });

    return <Evidence {...evidence} />;
  };
}
