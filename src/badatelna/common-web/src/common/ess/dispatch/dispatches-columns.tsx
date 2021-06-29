import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { Dispatch } from '../ess-types';
import { useDispatchMethods, useDispatchStates } from '../ess-api';

/**
 * fixme: add document, subject
 */
export function useColumns(): TableColumn<Dispatch>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'state',
      name: intl.formatMessage({
        id: 'ESS_DISPATCHES_COLUMN_STATE',
        defaultMessage: 'Stav',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useDispatchStates
      ),
      valueMapper: TableCells.useSelectCellFactory(useDispatchStates),
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'method',
      name: intl.formatMessage({
        id: 'ESS_DISPATCHES_COLUMN_METHOD',
        defaultMessage: 'Způsob odeslání',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useDispatchMethods
      ),
      valueMapper: TableCells.useSelectCellFactory(useDispatchMethods),
      sortable: true,
      filterable: true,
    },
  ];
}
