import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { Run } from '../schedule-types';
import { useRunStates, useJobsAll } from '../schedule-api';

export function useColumns(): TableColumn<Run>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'job',
      sortkey: 'job.name',
      filterkey: 'job.id',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_RUNS_COLUMN_JOB',
        defaultMessage: 'Úloha',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(useJobsAll),
      valueMapper: TableCells.dictionaryColumnMapper,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'state',
      sortkey: 'state.name',
      filterkey: 'state.id',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_RUNS_COLUMN_STATE',
        defaultMessage: 'Stav',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useRunStates
      ),
      valueMapper: TableCells.useSelectCellFactory(useRunStates),
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'startTime',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_RUNS_COLUMN_START_TIME',
        defaultMessage: 'Začátek',
      }),
      width: 150,
      CellComponent: TableCells.DateTimeCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'endTime',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_RUNS_COLUMN_END_TIME',
        defaultMessage: 'Konec',
      }),
      width: 150,
      CellComponent: TableCells.DateTimeCell,
      sortable: true,
      filterable: true,
    },
  ];
}
