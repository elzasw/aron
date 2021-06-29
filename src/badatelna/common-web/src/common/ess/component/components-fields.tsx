import React from 'react';
import { FormattedMessage } from 'react-intl';
import { FormPanel } from 'composite/form/fields/form-panel';
import { Identifier } from '../common/identifier';
import { useComponentTypes } from '../ess-api';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';
import { FormSelect } from 'composite/form/fields/form-select';
import { FormFileField } from 'composite/form/fields/form-file-field';

/**
 * fixme: add document
 */
export function ComponentsFields() {
  const types = useComponentTypes();

  return (
    <>
      <FormPanel
        label={
          <FormattedMessage
            id="ESS_COMPONENTS_PANEL_TITLE"
            defaultMessage="Komponenta"
          />
        }
      >
        <FormDateTimeField
          name="syncTime"
          label={
            <FormattedMessage
              id="ESS_COMPONENTS_FIELD_LABEL_RECORD_SYNC_TIME"
              defaultMessage="Čas synchronizace"
            />
          }
        />
        <FormSelect
          name="type"
          label={
            <FormattedMessage
              id="ESS_COMPONENTS_FIELD_LABEL_TYPE"
              defaultMessage="Type"
            />
          }
          valueIsId={true}
          source={types}
          tooltipMapper={(o) => o.name}
        />
        <FormFileField
          name="file"
          label={
            <FormattedMessage
              id="ESS_COMPONENTS_FIELD_LABEL_FILE"
              defaultMessage="Soubor"
            />
          }
        />
      </FormPanel>
      <Identifier />
    </>
  );
}
