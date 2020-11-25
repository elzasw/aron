import React, { ComponentType } from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormNumberField } from 'composite/form/fields/form-number-field';
import { FormCheckbox } from 'composite/form/fields/form-checkbox';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';

export function DictionaryFields(options: { FieldsComponent?: ComponentType }) {
  return (
    <>
      {options.FieldsComponent && <options.FieldsComponent />}
      <FormTextField
        name="name"
        required
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_NAME"
            defaultMessage="Název"
          />
        }
      />
      <FormNumberField
        name="order"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_ORDER"
            defaultMessage="Pořadí"
          />
        }
      />
      <FormCheckbox
        name="active"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_ACTIVE"
            defaultMessage="Aktivní"
          />
        }
        disabled={true}
      />
      <FormDateTimeField
        name="validFrom"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_VALID_FROM"
            defaultMessage="Platný od"
          />
        }
      />
      <FormDateTimeField
        name="validTo"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_VALID_TO"
            defaultMessage="Platný do"
          />
        }
      />
      <FormTextField
        name="code"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_CODE"
            defaultMessage="Kód"
          />
        }
      />
    </>
  );
}
