import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';
import { Identifier } from '../../common/identifier';
import { FormNumberField } from 'composite/form/fields/form-number-field';

export function ShreddingModesFields() {
  return (
    <>
      <FormPanel
        label={
          <FormattedMessage
            id="ESS_RECORDS_PANEL_TITLE"
            defaultMessage="Skartaní režim"
          />
        }
      >
        <FormTextField
          name="symbol"
          label={
            <FormattedMessage
              id="ESS_SHREDDING_MODES_FIELD_LABEL_SYMBOL"
              defaultMessage="Skartační znak"
            />
          }
        />
        <FormNumberField
          name="period"
          label={
            <FormattedMessage
              id="ESS_SHREDDING_MODES_FIELD_LABEL_PERIOD"
              defaultMessage="Skartační lhůta"
            />
          }
        />
      </FormPanel>
      <Identifier />
    </>
  );
}
