import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { FormTextField } from 'composite/form/fields/form-text-field';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormSelect } from 'composite/form/fields/form-select';
import { FormEditor } from 'composite/form/fields/form-editor';
import { useFormSelector } from 'composite/form/selectors/selector';
import { Job } from '../schedule-types';
import { ScriptType } from 'common/common-types';
import { useScriptTypes } from '../schedule-api';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';

export function JobsFields() {
  const intl = useIntl();

  const scriptType = useFormSelector((data: Job) => data.scriptType);

  const types = useScriptTypes();

  return (
    <FormPanel
      label={
        <FormattedMessage
          id="EAS_SCHEDULE_JOBS_PANEL_TITLE"
          defaultMessage="Časová úloha"
        />
      }
    >
      <FormTextField
        name="timer"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_JOBS_FIELD_LABEL_TIMER"
            defaultMessage="Časovač"
          />
        }
      />
      <FormDateTimeField
        name="last"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_JOBS_FIELD_LABEL_LAST"
            defaultMessage="Poslední spuštění"
          />
        }
        disabled={true}
      />
      <FormDateTimeField
        name="next"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_JOBS_FIELD_LABEL_NEXT"
            defaultMessage="Příští spuštění"
          />
        }
        disabled={true}
      />
      <FormTextField
        name="description"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_JOBS_FIELD_LABEL_DESCRIPTION"
            defaultMessage="Popis"
          />
        }
      />
      <FormSelect
        name="scriptType"
        required
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_JOBS_FIELD_LABEL_SCRIPT_TYPE"
            defaultMessage="Skriptovací jazyk"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SCHEDULE_JOBS_FIELD_HELP_SCRIPT_TYPE',
          defaultMessage: ' ',
        })}
        source={types}
        tooltipMapper={(o) => o.name}
        valueIsId={true}
      />
      <FormEditor
        name="script"
        required
        disabled={scriptType == null}
        language={scriptType === ScriptType.GROOVY ? 'java' : 'javascript'}
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_JOBS_FIELD_LABEL_SCRIPT"
            defaultMessage="Skript"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SCHEDULE_JOBS_FIELD_HELP_SCRIPT',
          defaultMessage: ' ',
        })}
      />
    </FormPanel>
  );
}
