import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormNumberField } from 'composite/form/fields/form-number-field';
import { FormCheckbox } from 'composite/form/fields/form-checkbox';
import { FormPanel } from 'composite/form/fields/form-panel';

export function SequencesFields() {
  return (
    <FormPanel
      label={
        <FormattedMessage
          id="EAS_SEQUENCES_PANEL_TITLE"
          defaultMessage="Číselná řada"
        />
      }
    >
      <FormTextField
        name="description"
        label={
          <FormattedMessage
            id="EAS_SEQUENCES_FIELD_LABEL_DESCRIPTION"
            defaultMessage="Popis"
          />
        }
      />
      <FormTextField
        name="format"
        required
        label={
          <FormattedMessage
            id="EAS_SEQUENCES_FIELD_LABEL_FORMAT"
            defaultMessage="Formát"
          />
        }
      />
      <FormNumberField
        name="counter"
        required
        label={
          <FormattedMessage
            id="EAS_SEQUENCES_FIELD_LABEL_COUNTER"
            defaultMessage="Počítadlo"
          />
        }
      />
      <FormCheckbox
        name="local"
        required
        label={
          <FormattedMessage
            id="EAS_SEQUENCES_FIELD_LABEL_LOCAL"
            defaultMessage="Lokální řada"
          />
        }
      />
    </FormPanel>
  );
}
