import React, { ComponentType } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormNumberField } from 'composite/form/fields/form-number-field';
import { FormCheckbox } from 'composite/form/fields/form-checkbox';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';

export function DictionaryFields(options: { FieldsComponent?: ComponentType }) {
  const intl = useIntl();

  return (
    <>
      <FormNumberField
        name="order"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_ORDER"
            defaultMessage="Pořadí"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EVIDENCE_FIELD_HELP_ORDER',
          defaultMessage: ' ',
        })}
      />
      <FormTextField
        name="name"
        required
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_NAME"
            defaultMessage="Název"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EVIDENCE_FIELD_HELP_NAME',
          defaultMessage: ' ',
        })}
      />
      <FormCheckbox
        name="active"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_ACTIVE"
            defaultMessage="Aktivní"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EVIDENCE_FIELD_HELP_ACTIVE',
          defaultMessage: ' ',
        })}
        disabled={true}
      />
      {options.FieldsComponent && <options.FieldsComponent />}
      <FormDateTimeField
        name="validFrom"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_VALID_FROM"
            defaultMessage="Platný od"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EVIDENCE_FIELD_HELP_VALID_FROM',
          defaultMessage: ' ',
        })}
      />
      <FormDateTimeField
        name="validTo"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_VALID_TO"
            defaultMessage="Platný do"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EVIDENCE_FIELD_HELP_VALID_TO',
          defaultMessage: ' ',
        })}
      />
      <FormTextField
        name="code"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_CODE"
            defaultMessage="Kód"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EVIDENCE_FIELD_HELP_CODE',
          defaultMessage: ' ',
        })}
      />
    </>
  );
}
