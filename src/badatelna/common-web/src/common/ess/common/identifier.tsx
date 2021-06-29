import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';

export function Identifier() {
  return (
    <FormPanel
      label={
        <FormattedMessage
          id="ESS_IDENTIFIER_PANEL_TITLE"
          defaultMessage="Identifikátor"
        />
      }
    >
      <FormTextField
        name="essId.source"
        label={
          <FormattedMessage
            id="ESS_IDENTIFIER_FIELD_LABEL_SOURCE"
            defaultMessage="Zdroj"
          />
        }
      />
      <FormTextField
        name="essId.id"
        label={
          <FormattedMessage
            id="ESS_IDENTIFIER_FIELD_LABEL_ID"
            defaultMessage="Id"
          />
        }
      />
    </FormPanel>
  );
}
