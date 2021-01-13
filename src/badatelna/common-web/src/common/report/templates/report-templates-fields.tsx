import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import {
  ListSource,
  DictionaryAutocomplete,
  ReportProvider,
} from 'common/common-types';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormFileField } from 'composite/form/fields/form-file-field';
import { FormSelect } from 'composite/form/fields/form-select';
import { FormEditor } from 'composite/form/fields/form-editor';

export function reportTemplatesFieldsFactory(
  useReportTags: () => ListSource<DictionaryAutocomplete>,
  useReportProviders: () => ListSource<ReportProvider>
) {
  return function ReportTemplatesFields() {
    const intl = useIntl();

    const providers = useReportProviders();
    const tags = useReportTags();

    return (
      <FormPanel
        label={
          <FormattedMessage
            id="EAS_REPORT_TEMPLATES_FIELD_PANEL_TITLE"
            defaultMessage="Tisková šablona"
          />
        }
      >
        <FormTextField
          name="label"
          label={
            <FormattedMessage
              id="EAS_REPORT_TEMPLATES_FIELD_LABEL_LABEL"
              defaultMessage="Označení"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_TEMPLATES_FIELD_HELP_LABEL',
            defaultMessage: 'Použije je v seznamu šablon v dialogu tisku',
          })}
        />
        <FormFileField
          name="content"
          required
          label={
            <FormattedMessage
              id="EAS_REPORT_TEMPLATES_FIELD_LABEL_CONTENT"
              defaultMessage="Obsah"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_TEMPLATES_FIELD_HELP_CONTENT',
            defaultMessage: ' ',
          })}
        />
        <FormSelect
          name="provider"
          required
          label={
            <FormattedMessage
              id="EAS_REPORT_TEMPLATES_FIELD_LABEL_PROVIDER"
              defaultMessage="Poskytoval dat"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_TEMPLATES_FIELD_HELP_PROVIDER',
            defaultMessage: ' ',
          })}
          valueIsId={true}
          source={providers}
          tooltipMapper={(o) => o.name}
        />
        <FormEditor
          name="configuration"
          required
          language={'json'}
          label={
            <FormattedMessage
              id="EAS_REPORT_TEMPLATES_FIELD_LABEL_CONFIGURATION"
              defaultMessage="Parametry"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_TEMPLATES_FIELD_HELP_CONFIGURATION',
            defaultMessage: ' ',
          })}
        />
        <FormSelect
          name="tags"
          required
          label={
            <FormattedMessage
              id="EAS_REPORT_TEMPLATES_FIELD_LABEL_TAGS"
              defaultMessage="Umístění"
            />
          }
          helpLabel={intl.formatMessage({
            id: 'EAS_REPORT_TEMPLATES_FIELD_HELP_TAGS',
            defaultMessage: ' ',
          })}
          multiple={true}
          valueIsId={true}
          source={tags}
          tooltipMapper={(o) => o.name}
        />
      </FormPanel>
    );
  };
}
