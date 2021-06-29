import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormNumberField } from 'composite/form/fields/form-number-field';

export function ShreddingMode() {
  return (
    <FormPanel
      label={
        <FormattedMessage
          id="ESS_SHREDING_MODE_PANEL_TITLE"
          defaultMessage="Skartační režim"
        />
      }
    >
      <FormTextField
        name="shreddingMode.symbol"
        label={
          <FormattedMessage
            id="ESS_SHREDING_MODE_FIELD_LABEL_SYMBOL"
            defaultMessage="Skartační znak"
          />
        }
      />
      <FormNumberField
        name="shreddingMode.period"
        label={
          <FormattedMessage
            id="ESS_SHREDING_MODE_FIELD_LABEL_PERIOD"
            defaultMessage="Skartační lhůta"
          />
        }
      />
    </FormPanel>
  );
}
