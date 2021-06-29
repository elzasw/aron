import React, { useMemo } from 'react';
import { useIntl } from 'react-intl';
import { stubFalse } from 'lodash';
import { useDatedEvidence } from 'composite/evidence/dated-evidence/dated-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { JobsFields } from './runs-fields';
import { useColumns } from './runs-columns';
import { Run } from '../schedule-types';
import { ScheduleRunsContext } from './runs-context';
import { TableSort } from 'composite/table/table-types';

export function scheduleRunsFactory(
  url: string,
  jobsUrl: string,
  reportTag: string | null,
  defaultSorts?: TableSort[]
) {
  return function Runs() {
    const intl = useIntl();

    const evidence = useDatedEvidence<Run>({
      identifier: 'SCHEDULE_RUNS',
      apiProps: {
        url,
      },
      tableProps: {
        columns: useColumns(),
        tableName: intl.formatMessage({
          id: 'EAS_SCHEDULE_RUNS_TABLE_TITLE',
          defaultMessage: 'Běhy časových úloh',
        }),
        reportTag,
        defaultSorts,
      },
      detailProps: {
        FieldsComponent: JobsFields,
        toolbarProps: {
          showButton: stubFalse,
        },
      },
    });

    const context: ScheduleRunsContext = useMemo(
      () => ({
        jobsUrl,
      }),
      []
    );

    return (
      <ScheduleRunsContext.Provider value={context}>
        <Evidence {...evidence} />
      </ScheduleRunsContext.Provider>
    );
  };
}
