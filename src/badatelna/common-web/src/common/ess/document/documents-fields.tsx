import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';
import { Identifier } from '../common/identifier';
import { ShreddingMode } from '../common/shredding-mode';
import { FormTableField } from 'composite/form/fields/form-table-field';
import { useComponentColumns } from './documents-columns';
import { stubFalse } from 'lodash';

/**
 * fixme: add record and components, type
 */
export function DocumentsFields() {
  return (
    <>
      <FormPanel
        label={
          <FormattedMessage
            id="ESS_DOCUMENTS_PANEL_TITLE"
            defaultMessage="Dokument"
          />
        }
      >
        <FormTextField
          name="referenceNumber"
          label={
            <FormattedMessage
              id="ESS_DOCUMENTS_FIELD_LABEL_RECORD_REFERENCE_NUMBER"
              defaultMessage="Číslo jednací"
            />
          }
        />
        <FormTextField
          name="name"
          label={
            <FormattedMessage
              id="ESS_DOCUMENTS_FIELD_LABEL_NAME"
              defaultMessage="Název"
            />
          }
        />
        <FormTextField
          name="description"
          label={
            <FormattedMessage
              id="ESS_DOCUMENTS_FIELD_LABEL_DESCRIPTION"
              defaultMessage="Popis"
            />
          }
        />
        <FormTextField
          name="classificationCode"
          label={
            <FormattedMessage
              id="ESS_DOCUMENTS_FIELD_LABEL_RECORD_CLASSIFICATION_CODE"
              defaultMessage="Spisový znak"
            />
          }
        />
        <FormTextField
          name="barCode"
          label={
            <FormattedMessage
              id="ESS_DOCUMENTS_FIELD_LABEL_RECORD_BARCODE"
              defaultMessage="Čárový kód"
            />
          }
        />
      </FormPanel>
      <Identifier />
      <ShreddingMode />
      <FormPanel
        label={
          <FormattedMessage
            id="ESS_DOCUMENTS_FIELD_COMPONENTS_PANEL_TITLE"
            defaultMessage="Soubory"
          />
        }
      >
        <FormTableField
          name="components"
          columns={useComponentColumns()}
          showToolbar={false}
          showDetailBtnCond={stubFalse}
          labelOptions={{ hide: true }}
        />
      </FormPanel>
    </>
  );
}
