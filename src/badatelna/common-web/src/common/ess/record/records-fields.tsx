import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';
import { Identifier } from '../common/identifier';
import { ShreddingMode } from '../common/shredding-mode';

/**
 * fixme: add iniciacni dokument
 */
export function RecordsFields() {
  return (
    <>
      <FormPanel
        label={
          <FormattedMessage
            id="ESS_RECORDS_PANEL_TITLE"
            defaultMessage="Spis"
          />
        }
      >
        <FormTextField
          name="recordSymbol"
          label={
            <FormattedMessage
              id="ESS_RECORDS_FIELD_LABEL_RECORD_SYMBOL"
              defaultMessage="Spisová značka"
            />
          }
        />
        <FormTextField
          name="name"
          label={
            <FormattedMessage
              id="ESS_RECORDS_FIELD_LABEL_NAME"
              defaultMessage="Název"
            />
          }
        />
        <FormTextField
          name="description"
          label={
            <FormattedMessage
              id="ESS_RECORDS_FIELD_LABEL_DESCRIPTION"
              defaultMessage="Popis"
            />
          }
        />
        <FormTextField
          name="classificationCode"
          label={
            <FormattedMessage
              id="ESS_RECORDS_FIELD_LABEL_RECORD_CLASSIFICATION_CODE"
              defaultMessage="Spisový znak"
            />
          }
        />
        <FormTextField
          name="barCode"
          label={
            <FormattedMessage
              id="ESS_RECORDS_FIELD_LABEL_RECORD_BARCODE"
              defaultMessage="Čárový kód"
            />
          }
        />
      </FormPanel>
      <Identifier />
      <ShreddingMode />
    </>
  );
}
