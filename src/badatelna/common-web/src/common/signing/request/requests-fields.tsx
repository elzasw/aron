import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormTableField } from 'composite/form/fields/form-table-field';
import { useContentColumns } from './requests-columns';
import { FormSelect } from 'composite/form/fields/form-select';
import { useRequestStates } from '../signing-api';
import { FormFileField } from 'composite/form/fields/form-file-field';

export function RequestsFields() {
  const intl = useIntl();

  const states = useRequestStates();

  return (
    <>
      <FormPanel
        label={
          <FormattedMessage
            id="EAS_SIGNING_REQUESTS_PANEL_TITLE"
            defaultMessage="Dokument"
          />
        }
      >
        <FormTextField
          name="name"
          label={
            <FormattedMessage
              id="EAS_SIGNING_REQUESTS_FIELD_LABEL_NAME"
              defaultMessage="Název"
            />
          }
        />
        <FormTextField
          name="user.name"
          disabled
          label={
            <FormattedMessage
              id="EAS_SIGNING_REQUESTS_FIELD_LABEL_USER"
              defaultMessage="Uživatel"
            />
          }
        />
        <FormSelect
          name="state"
          disabled
          label={
            <FormattedMessage
              id="EAS_SIGNING_REQUESTS_FIELD_LABEL_STATE"
              defaultMessage="Stav"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_SIGNING_REQUESTS_FIELD_HELP_STATE',
            defaultMessage: ' ',
          })}
          source={states}
          tooltipMapper={(o) => o.name}
          valueIsId={true}
        />
        <FormTextField
          name="error"
          disabled
          label={
            <FormattedMessage
              id="EAS_SIGNING_REQUESTS_FIELD_LABEL_ERROR"
              defaultMessage="Chyba"
            />
          }
        />
      </FormPanel>
      <FormPanel
        label={
          <FormattedMessage
            id="EAS_SIGNING_REQUESTS_CONTENTS_PANEL_TITLE"
            defaultMessage="Soubory"
          />
        }
      >
        <FormTableField
          name="contents"
          columns={useContentColumns()}
          FormFieldsComponent={ContentFields}
          labelOptions={{ hide: true }}
        />
      </FormPanel>
    </>
  );
}

export function ContentFields() {
  const intl = useIntl();

  return (
    <>
      <FormFileField
        name="toSign"
        label={
          <FormattedMessage
            id="EAS_SIGNING_CONTENTS_FIELD_LABEL_TO_SIGN"
            defaultMessage="K podpisu"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SIGNING_CONTENTS_FIELD_HELP_TO_SIGN',
          defaultMessage: ' ',
        })}
      />
      <FormFileField
        name="signed"
        disabled
        label={
          <FormattedMessage
            id="EAS_SIGNING_CONTENTS_FIELD_LABEL_SIGNED"
            defaultMessage="K podpisu"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SIGNING_CONTENTS_FIELD_HELP_SIGNED',
          defaultMessage: ' ',
        })}
      />
    </>
  );
}
