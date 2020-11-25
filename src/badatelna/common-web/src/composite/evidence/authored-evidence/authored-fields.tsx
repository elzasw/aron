import React, { ComponentType } from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';

export function AuthoredFields(options: { FieldsComponent?: ComponentType }) {
  return (
    <>
      {options.FieldsComponent && <options.FieldsComponent />}
      <FormTextField
        name="createdBy.name"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_CREATED_BY"
            defaultMessage="Autor"
          />
        }
        disabled={true}
      />
      <FormTextField
        name="updatedBy.name"
        label={
          <FormattedMessage
            id="EAS_EVIDENCE_FIELD_LABEL_UPDATED_BY"
            defaultMessage="Autor úpravy"
          />
        }
        disabled={true}
      />
    </>
  );
}
