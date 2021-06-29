import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormEditor } from 'composite/form/fields/form-editor';

export function MessagesFields() {
  return (
    <FormPanel
      label={
        <FormattedMessage
          id="EAS_SOAP_LOGGER_MESSAGES_PANEL_TITLE"
          defaultMessage="SOAP komunikace"
        />
      }
    >
      <FormTextField
        name="service"
        label={
          <FormattedMessage
            id="EAS_SOAP_LOGGER_MESSAGES_LABEL_SERVICE"
            defaultMessage="Služba"
          />
        }
      />
      <FormEditor
        name="request"
        language="xml"
        disabled
        label={
          <FormattedMessage
            id="EAS_SOAP_LOGGER_MESSAGES_LABEL_REQUEST"
            defaultMessage="Dotaz"
          />
        }
      />
      <FormEditor
        name="response"
        language="xml"
        disabled
        label={
          <FormattedMessage
            id="EAS_SOAP_LOGGER_MESSAGES_LABEL_RESPONSE"
            defaultMessage="Odpověď"
          />
        }
      />
    </FormPanel>
  );
}
