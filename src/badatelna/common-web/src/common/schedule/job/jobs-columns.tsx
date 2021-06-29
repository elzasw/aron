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
