import React, { useContext, useMemo } from 'react';
import { useIntl } from 'react-intl';
import { useDictionaryEvidence } from 'composite/evidence/dictionary-evidence/dictionary-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { JobsFields } from './jobs-fields';
import { useColumns } from './jobs-columns';
import { useValidationSchema } from './jobs-schema';
import { Job } from '../schedule-types';
import { jobsToolbarFactory } from './jobs-toolbar';
import { TableSort } from 'composite/table/table-types';
import { ScheduleContext } from '../schedule-context';

export function scheduleJobsFactory(
  reportTag: string | null,
  defaultSorts?: TableSort[]
) {
  return function Jobs() {
    const intl = useIntl();
    const validationSchema = useValidationSchema();
    const { jobUrl: url } = useContext(ScheduleContext);

    const JobsToolbar = useMemo(() => jobsToolbarFactory({}), []);

    const evidence = useDictionaryEvidence<Job>({
      identifier: 'SCHEDULE_JOBS',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'EAS_SCHEDULE_JOBS_TABLE_TITLE',
          defaultMessage: 'Časové úlohy',
        }),
        reportTag,
        defaultSorts,
      },
      detailProps: {
        toolbarProps: {
          after: <JobsToolbar />,
        },
        FieldsComponent: JobsFields,
        validationSchema,
      },
    });

    return <Evidence {...evidence} />;
  };
}

scheduleJobsFactory.useColumns = useColumns;
scheduleJobsFactory.Fields = JobsFields;
scheduleJobsFactory.useValidationSchema = useValidationSchema;
scheduleJobsFactory.detailToolbarFactory = jobsToolbarFactory;
