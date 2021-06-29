import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { Job } from '../schedule-types';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { useScriptTypes } from '../schedule-api';

export function useColumns(): TableColumn<Job>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'timer',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_JOBS_COLUMN_TIMER',
        defaultMessage: 'Časovač',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'running',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_JOBS_COLUMN_RUNNING',
        defaultMessage: 'Běží',
      }),
      width: 100,
      CellComponent: TableCells.BooleanCell,
      sortable: false,
      filterable: false, // neni možno filtrovat a sortovat, protože informace se dopočítavá za běhu
    },
    {
      datakey: 'last',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_JOBS_COLUMN_LAST',
        defaultMessage: 'Poslední spuštění',
      }),
      width: 150,
      CellComponent: TableCells.DateTimeCell,
      FilterComponent: TableFilterCells.FilterDateCell,
      sortable: true,
      filterable: true,
      filterGroup: 10,
    },
    {
      datakey: 'next',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_JOBS_COLUMN_NEXT',
        defaultMessage: 'Příští spuštění',
      }),
      width: 150,
      CellComponent: TableCells.DateTimeCell,
      FilterComponent: TableFilterCells.FilterDateCell,
      sortable: true,
      filterable: true,
      filterGroup: 10,
    },
    {
      datakey: 'scriptType',
      sortkey: 'scriptType.name',
      filterkey: 'scriptType.id',
      name: intl.formatMessage({
        id: 'EAS_SCHEDULE_JOBS_COLUMN_SCRIPT_TYPE',
        defaultMessage: 'Skriptovací jazyk',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useScriptTypes
      ),
      valueMapper: TableCells.useSelectCellFactory(useScriptTypes),
      sortable: true,
      filterable: true,
    },
  ];
}
