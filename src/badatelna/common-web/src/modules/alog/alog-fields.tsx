import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormEditor } from 'composite/form/fields/form-editor';
import { FormSelect } from 'composite/form/fields/form-select';
import { useEventSourceTypes } from './alog-api';

export function AlogFields() {
  const sourceTypes = useEventSourceTypes();

  return (
    <FormPanel
      label={
        <FormattedMessage id="EAS_ALOG_PANEL_TITLE" defaultMessage="Událost" />
      }
    >
      <FormSelect
        name="sourceType"
        label={
          <FormattedMessage
            id="EAS_ALOG_FIELD_LABEL_SOURCE_TYPE"
            defaultMessage="Typ zdroje"
          />
        }
        source={sourceTypes}
        tooltipMapper={(o) => o.name}
        valueIsId={true}
      />
      <FormTextField
        name="source"
        label={
          <FormattedMessage
            id="EAS_ALOG_FIELD_LABEL_SOURCE"
            defaultMessage="Zdroj"
          />
        }
      />
      <FormTextField
        name="module.name"
        label={
          <FormattedMessage
            id="EAS_ALOG_FIELD_LABEL_MODULE"
            defaultMessage="Modul"
          />
        }
      />
      <FormTextField
        name="message"
        label={
          <FormattedMessage
            id="EAS_ALOG_FIELD_LABEL_MESSAGE"
            defaultMessage="Správa"
          />
        }
      />
      <FormTextField
        name="ipAddress"
        label={
          <FormattedMessage
            id="EAS_ALOG_FIELD_LABEL_IP_ADDRESS"
            defaultMessage="IP adresa"
          />
        }
      />
      <FormTextField name="user.name" label="Uživatel" />
      <FormEditor
        name="detail"
        label={
          <FormattedMessage
            id="EAS_ALOG_FIELD_LABEL_DETAIL"
            defaultMessage="JSON detail"
          />
        }
      />
    </FormPanel>
  );
}
