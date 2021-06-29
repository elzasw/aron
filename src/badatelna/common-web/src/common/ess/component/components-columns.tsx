import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { Component } from '../ess-types';
import { useComponentTypes } from '../ess-api';

/**
 * fixme: add document
 */
export function useColumns(): TableColumn<Component>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'syncTime',
      name: intl.formatMessage({
        id: 'ESS_COMPONENTS_COLUMN_SYNC_TIME',
        defaultMessage: 'Čas synchronizace',
      }),
      width: 200,
      CellComponent: TableCells.DateTimeCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'type',
      name: intl.formatMessage({
        id: 'ESS_COMPONENTS_COLUMN_TYPE',
        defaultMessage: 'Typ',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useComponentTypes
      ),
      valueMapper: TableCells.useSelectCellFactory(useComponentTypes),
      sortable: true,
      filterable: true,
    },
  ];
}
