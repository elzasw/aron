import React, { useContext, useMemo } from 'react';
import { useIntl } from 'react-intl';
import ButtonGroup from '@material-ui/core/ButtonGroup/ButtonGroup';
import { useStyles } from './styles';
import { DetailHandle } from 'composite/detail/detail-types';
import {
  SignRequest,
  SignRequestState,
  UploadSignedContentDto,
  SignContent,
} from '../signing-types';
import { DetailContext } from 'composite/detail/detail-context';
import { DetailToolbarButtonAction } from 'composite/detail/detail-toolbar-button-action';
import { signFactory, uploadSignedContentFactory } from '../signing-api';
import { SigningContext } from '../signing-context';
import { FormFileField } from 'composite/form/fields/form-file-field';
import { FormSelect } from 'composite/form/fields/form-select';
import { useStaticListSource } from 'utils/list-source-hook';

export function SignRequestToolbar() {
  const intl = useIntl();
  const classes = useStyles();
  const { source } = useContext<DetailHandle<SignRequest>>(DetailContext);
  const { url } = useContext(SigningContext);

  const sign = useMemo(() => signFactory(url), [url]);
  const uploadSignedContent = useMemo(() => uploadSignedContentFactory(url), [
    url,
  ]);

  const contentsSource = useStaticListSource<SignContent>(
    source.data?.contents ?? []
  );

  const UploadSignedContentFields = useMemo(
    () =>
      function UploadSignedContentFields() {
        return (
          <>
            <FormSelect
              name="content"
              label="K podepsání"
              source={contentsSource}
              labelMapper={(content) => content.toSign?.name ?? ''}
            />
            <FormFileField name="signed" label="Podepsaný soubor" required />
          </>
        );
      },
    [contentsSource]
  );

  return (
    <>
      <>
        <ButtonGroup
          size="small"
          variant="outlined"
          className={classes.toolbarIndentLeft}
        >
          {source.data?.state === SignRequestState.NEW && (
            <DetailToolbarButtonAction<UploadSignedContentDto>
              promptKey="SIGN_REQUEST_UPLOAD_SIGNED_CONTENT"
              apiCall={uploadSignedContent}
              formInitialValues={{ content: null, signed: null }}
              FormFields={UploadSignedContentFields}
              buttonLabel={intl.formatMessage({
                id: 'EAS_SIGN_REQUESTS_TOOLBAR_BTN_UPLOAD_SIGNED_CONTENT',
                defaultMessage: 'Nahrát',
              })}
              buttonTooltip={intl.formatMessage({
                id: 'EAS_SIGN_REQUESTS_TOOLBAR_TOOLTIP_UPLOAD_SIGNED_CONTENT',
                defaultMessage: 'Otevrě dialog s nahráním podepsané přílohy',
              })}
              dialogTitle={intl.formatMessage({
                id: 'EAS_SIGN_REQUESTS_UPLOAD_SIGNED_CONTENT_DIALOG_TITLE',
                defaultMessage: 'Varování',
              })}
              dialogText={intl.formatMessage({
                id: 'EAS_SIGN_REQUESTS_UPLOAD_SIGNED_CONTENT_DIALOG_TEXT',
                defaultMessage:
                  'Skutečně chcete označit požadavek za podepsaný ?',
              })}
            />
          )}
          {source.data?.state === SignRequestState.NEW && (
            <DetailToolbarButtonAction
              promptKey="SIGN_REQUEST_SIGN"
              apiCall={sign}
              formInitialValues={{ stampNumber: '' }}
              buttonLabel={intl.formatMessage({
                id: 'EAS_SIGN_REQUESTS_TOOLBAR_BTN_SIGN',
                defaultMessage: 'Podepsat',
              })}
              buttonTooltip={intl.formatMessage({
                id: 'EAS_SIGN_REQUESTS_TOOLBAR_TOOLTIP_SIGN',
                defaultMessage:
                  'Otevrě dialog s potvrzením podepsání dokumentu',
              })}
              dialogTitle={intl.formatMessage({
                id: 'EAS_SIGN_REQUESTS_SIGN_DIALOG_TITLE',
                defaultMessage: 'Varování',
              })}
              dialogText={intl.formatMessage({
                id: 'EAS_SIGN_REQUESTS_SIGN_DIALOG_TEXT',
                defaultMessage:
                  'Skutečně chcete označit požadavek za podepsaný ?',
              })}
            />
          )}
        </ButtonGroup>
      </>
    </>
  );
}
