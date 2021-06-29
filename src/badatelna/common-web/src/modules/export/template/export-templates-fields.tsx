import React, { useContext } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormFileField } from 'composite/form/fields/form-file-field';
import { FormSelect } from 'composite/form/fields/form-select';
import { FormEditor } from 'composite/form/fields/form-editor';
import { FormCheckbox } from 'composite/form/fields/form-checkbox';
import {
  useExportDataProviders,
  useExportDesignProviders,
  useExportTypes,
} from '../export-api';
import { ExportContext } from '../export-context';

export function ExportTemplatesFields() {
  const { tags } = useContext(ExportContext);

  const intl = useIntl();

  const dataProviders = useExportDataProviders();
  const designProviders = useExportDesignProviders();
  const types = useExportTypes();

  return (
    <FormPanel
      label={
        <FormattedMessage
          id="EAS_EXPORT_TEMPLATES_FIELD_PANEL_TITLE"
          defaultMessage="Tisková šablona"
        />
      }
    >
      <FormTextField
        name="label"
        label={
          <FormattedMessage
            id="EAS_EXPORT_TEMPLATES_FIELD_LABEL_LABEL"
            defaultMessage="Označení"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_FIELD_HELP_LABEL',
          defaultMessage: 'Použije je v seznamu šablon v dialogu tisku',
        })}
      />
      <FormFileField
        name="content"
        label={
          <FormattedMessage
            id="EAS_EXPORT_TEMPLATES_FIELD_LABEL_CONTENT"
            defaultMessage="Obsah"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_FIELD_HELP_CONTENT',
          defaultMessage: ' ',
        })}
      />
      <FormSelect
        name="dataProvider"
        required
        label={
          <FormattedMessage
            id="EAS_EXPORT_TEMPLATES_FIELD_LABEL_DATA_PROVIDER"
            defaultMessage="Zdroj dat"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_FIELD_HELP_DATA_PROVIDER',
          defaultMessage: ' ',
        })}
        valueIsId={true}
        source={dataProviders}
        tooltipMapper={(o) => o.name}
      />
      <FormSelect
        name="designProvider"
        required
        label={
          <FormattedMessage
            id="EAS_EXPORT_TEMPLATES_FIELD_LABEL_DESIGN_PROVIDER"
            defaultMessage="Zdroj designu"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_FIELD_HELP_DESIGN_PROVIDER',
          defaultMessage: ' ',
        })}
        valueIsId={true}
        source={designProviders}
        tooltipMapper={(o) => o.name}
      />
      <FormSelect
        name="allowedTypes"
        required
        label={
          <FormattedMessage
            id="EAS_EXPORT_TEMPLATES_FIELD_LABEL_ALLOWED_TYPES"
            defaultMessage="Povolené formáty"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_FIELD_HELP_ALLOWED_TYPES',
          defaultMessage: ' ',
        })}
        valueIsId={true}
        multiple
        source={types}
        tooltipMapper={(o) => o.name}
      />
      <FormEditor
        name="configuration"
        required
        language={'json'}
        label={
          <FormattedMessage
            id="EAS_EXPORT_TEMPLATES_FIELD_LABEL_CONFIGURATION"
            defaultMessage="Parametry"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_FIELD_HELP_CONFIGURATION',
          defaultMessage: ' ',
        })}
      />
      <FormSelect
        name="tags"
        required
        label={
          <FormattedMessage
            id="EAS_EXPORT_TEMPLATES_FIELD_LABEL_TAGS"
            defaultMessage="Umístění"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_FIELD_HELP_TAGS',
          defaultMessage: ' ',
        })}
        multiple={true}
        valueIsId={true}
        source={tags}
        tooltipMapper={(o) => o.name}
      />
      <FormCheckbox
        name="restrictByPermission"
        label={
          <FormattedMessage
            id="EAS_EXPORT_TEMPLATES_FIELD_LABEL_RESTRICT_BY_PERMISSION"
            defaultMessage="Omezit na oprávnění"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_EXPORT_TEMPLATES_FIELD_HELP_RESTRICT_BY_PERMISSION',
          defaultMessage: ' ',
        })}
        disabled={true}
      />
    </FormPanel>
  );
}
