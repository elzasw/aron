import React, { useContext } from 'react';
import { useIntl } from 'react-intl';
import { v4 as uuidv4 } from 'uuid';
import { useAuthoredEvidence } from 'composite/evidence/authored-evidence/authored-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { RequestsFields } from './requests-fields';
import { useColumns } from './requests-columns';
import { SignRequest, SignRequestState } from '../signing-types';
import { SignRequestToolbar } from './requests-detail-toolbar';
import { SigningContext } from '../signing-context';

export function SignRequests() {
  const intl = useIntl();

  const { url, reportTag } = useContext(SigningContext);

  const evidence = useAuthoredEvidence<SignRequest>({
    identifier: 'EAS_SIGNING_REQUEST',
    apiProps: {
      url,
    },
    tableProps: {
      columns: useColumns(),
      tableName: intl.formatMessage({
        id: 'EAS_SIGNING_REQUEST_TABLE_TITLE',
        defaultMessage: 'Dokumenty k podepsání',
      }),
      reportTag,
    },
    detailProps: {
      toolbarProps: {
        after: <SignRequestToolbar />,
      },
      FieldsComponent: RequestsFields,
      initNewItem: () =>
        ({
          id: uuidv4(),
          state: SignRequestState.NEW,
        } as SignRequest),
    },
  });

  return <Evidence {...evidence} />;
}
