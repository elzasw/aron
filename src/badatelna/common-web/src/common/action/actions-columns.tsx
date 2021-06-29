import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { Action } from './actions-types';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { useScriptTypes } from './actions-api';

export function useColumns(): TableColumn<Action>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'scriptType',
      sortkey: 'scriptType.name',
      filterkey: 'scriptType.id',
      name: intl.formatMessage({
        id: 'EAS_ACTIONS_COLUMN_SCRIPT_TYPE',
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
