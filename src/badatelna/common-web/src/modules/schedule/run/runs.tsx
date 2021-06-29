import React, { useContext } from 'react';
import { useIntl } from 'react-intl';
import { stubFalse } from 'lodash';
import { useDatedEvidence } from 'composite/evidence/dated-evidence/dated-evidence';
import { Evidence } from 'composite/evidence/evidence';
import { RunsFields } from './runs-fields';
import { useColumns } from './runs-columns';
import { Run } from '../schedule-types';
import { TableSort } from 'composite/table/table-types';
import { ScheduleContext } from '../schedule-context';

export function scheduleRunsFactory(
  reportTag: string | null,
  defaultSorts?: TableSort[]
) {
  return function Runs() {
    const intl = useIntl();
    const { runUrl: url } = useContext(ScheduleContext);

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
        FieldsComponent: RunsFields,
        toolbarProps: {
          showButton: stubFalse,
        },
      },
    });

    return <Evidence {...evidence} />;
  };
}

scheduleRunsFactory.useColumns = useColumns;
scheduleRunsFactory.Fields = RunsFields;
