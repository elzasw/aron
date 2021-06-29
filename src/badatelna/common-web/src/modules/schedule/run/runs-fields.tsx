import React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { FormPanel } from 'composite/form/fields/form-panel';
import { FormSelect } from 'composite/form/fields/form-select';
import { useJobs, useRunStates } from '../schedule-api';
import { FormDateTimeField } from 'composite/form/fields/form-date-time-field';
import { FormTextArea } from 'composite/form/fields/form-text-area';

export function RunsFields() {
  const intl = useIntl();

  const jobs = useJobs();
  const states = useRunStates();

  return (
    <FormPanel
      label={
        <FormattedMessage
          id="EAS_SCHEDULE_RUNS_PANEL_TITLE"
          defaultMessage="Běh časové úlohy"
        />
      }
    >
      <FormSelect
        name="job"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_RUNS_FIELD_LABEL_JOB"
            defaultMessage="Úloha"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SCHEDULE_RUNS_FIELD_HELP_JOB',
          defaultMessage: ' ',
        })}
        source={jobs}
        tooltipMapper={(o) => o.name}
      />
      <FormSelect
        name="state"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_RUNS_FIELD_LABEL_STATE"
            defaultMessage="Stav"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SCHEDULE_RUNS_FIELD_HELP_STATE',
          defaultMessage: ' ',
        })}
        source={states}
        tooltipMapper={(o) => o.name}
        valueIsId={true}
      />
      <FormDateTimeField
        name="startTime"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_RUNS_FIELD_LABEL_START_TIME"
            defaultMessage="Začátek"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SCHEDULE_RUNS_FIELD_HELP_START_TIME',
          defaultMessage: ' ',
        })}
      />
      <FormDateTimeField
        name="endTime"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_RUNS_FIELD_LABEL_END_TIME"
            defaultMessage="Konec"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SCHEDULE_RUNS_FIELD_HELP_END_TIME',
          defaultMessage: ' ',
        })}
      />
      <FormTextArea
        name="result"
        disabled
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_RUNS_FIELD_LABEL_RESULT"
            defaultMessage="Výsledek"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SCHEDULE_JOBS_FIELD_HELP_RESULT',
          defaultMessage: ' ',
        })}
      />
      <FormTextArea
        name="console"
        label={
          <FormattedMessage
            id="EAS_SCHEDULE_RUNS_FIELD_LABEL_CONSOLE"
            defaultMessage="Konzole"
          />
        }
        helpLabel={intl.formatMessage({
          id: 'EAS_SCHEDULE_JOBS_FIELD_HELP_CONSOLE',
          defaultMessage: ' ',
        })}
      />
    </FormPanel>
  );
}
