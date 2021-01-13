import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormSelect } from 'composite/form/fields/form-select';
import { FormEditor } from 'composite/form/fields/form-editor';
import { FormFileField } from 'composite/form/fields/form-file-field';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';
import { useReportRequestStates, useReportTypes } from './report-requests-api';

export function ReportRequestsFields() {
  const intl = useIntl();

  const states = useReportRequestStates();
  const types = useReportTypes();

  return (
    <>
      <FormPanel
        label={
          <FormattedMessage
            id="EAS_REPORT_REQUESTS_FIELD_PANEL_TITLE"
            defaultMessage="Požadavek na tisk"
          />
        }
      >
        <FormSelect
          name="state"
          required
          label={
            <FormattedMessage
              id="EAS_REPORT_REQUESTS_FIELD_LABEL_STATE"
              defaultMessage="Stav"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_REQUESTS_FIELD_HELP_STATE',
            defaultMessage: ' ',
          })}
          source={states}
          tooltipMapper={(o) => o.name}
          valueIsId={true}
        />
        <FormSelect
          name="type"
          required
          label={
            <FormattedMessage
              id="EAS_REPORT_REQUESTS_FIELD_LABEL_TYPE"
              defaultMessage="Typ výstupu"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_REQUESTS_FIELD_HELP_TYPE',
            defaultMessage: ' ',
          })}
          source={types}
          tooltipMapper={(o) => o.name}
          valueIsId={true}
        />
        <FormEditor
          name="configuration"
          language={'json'}
          label={
            <FormattedMessage
              id="EAS_REPORT_REQUESTS_FIELD_LABEL_CONFIGURATION"
              defaultMessage="Parametry"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_REQUESTS_FIELD_HELP_CONFIGURATION',
            defaultMessage: ' ',
          })}
        />
      </FormPanel>
      <FormPanel
        label={
          <FormattedMessage
            id="KS_O_REPORT_REQUESTS_FIELD_RESULT_PANEL_TITLE"
            defaultMessage="Výsledek"
          />
        }
      >
        <FormFileField
          name="result"
          label={
            <FormattedMessage
              id="EAS_REPORT_REQUESTS_FIELD_LABEL_RESULT"
              defaultMessage="Výstup"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_REQUESTS_FIELD_HELP_RESULT',
            defaultMessage: ' ',
          })}
        />
        <FormTextField
          name="message"
          label={
            <FormattedMessage
              id="EAS_REPORT_REQUESTS_FIELD_LABEL_MESSAGE"
              defaultMessage="Chybová správa"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_REQUESTS_FIELD_HELP_MESSAGE',
            defaultMessage: ' ',
          })}
        />
        <FormDateTimeField
          name="processingStart"
          label={
            <FormattedMessage
              id="EAS_REPORT_REQUESTS_FIELD_LABEL_PROCESSING_START"
              defaultMessage="Začátek zpracování"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_REQUESTS_FIELD_HELP_PROCESSING_START',
            defaultMessage: ' ',
          })}
        />
        <FormDateTimeField
          name="processingEnd"
          label={
            <FormattedMessage
              id="EAS_REPORT_REQUESTS_FIELD_LABEL_PROCESSING_END"
              defaultMessage="Konec zpracování"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_REQUESTS_FIELD_HELP_PROCESSING_END',
            defaultMessage: ' ',
          })}
        />
      </FormPanel>
    </>
  );
}
