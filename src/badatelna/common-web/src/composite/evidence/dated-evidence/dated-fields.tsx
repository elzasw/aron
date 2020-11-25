import React, { ComponentType } from 'react';
import { FormattedMessage } from 'react-intl';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';

export function DatedFields(options: { FieldsComponent?: ComponentType }) {
  return (
    <>
      {options.FieldsComponent && <options.FieldsComponent />}
      <FormDateTimeField
        name="created"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_CREATED"
            defaultMessage="Vytvoření"
          />
        }
        disabled={true}
      />
      <FormDateTimeField
        name="updated"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_UPDATED"
            defaultMessage="Poslední úprava"
          />
        }
        disabled={true}
      />
    </>
  );
}
