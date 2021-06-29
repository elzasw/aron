import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { AlogEvent } from './alog-types';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { useEventSourceTypes } from './alog-api';

export function useColumns(): TableColumn<AlogEvent>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'sourceType',
      sortkey: 'sourceType.name',
      filterkey: 'sourceType.id',
      name: intl.formatMessage({
        id: 'EAS_ALOG_COLUMN_SOURCE_TYPE',
        defaultMessage: 'Typ zdroje',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useEventSourceTypes
      ),
      valueMapper: TableCells.useSelectCellFactory(useEventSourceTypes),
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'source',
      name: intl.formatMessage({
        id: 'EAS_ALOG_COLUMN_SOURCE',
        defaultMessage: 'Zdroj',
      }),
      width: 100,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'module.name',
      name: intl.formatMessage({
        id: 'EAS_ALOG_COLUMN_MODULE',
        defaultMessage: 'Modul',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'message',
      name: intl.formatMessage({
        id: 'EAS_ALOG_COLUMN_MESSAGE',
        defaultMessage: 'Správa',
      }),
      width: 300,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'ipAddress',
      name: intl.formatMessage({
        id: 'EAS_ALOG_COLUMN_IP_ADDRESS',
        defaultMessage: 'IP adresa',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'user',
      displaykey: 'user.name',
      sortkey: 'user.name',
      filterkey: 'user.name',
      name: intl.formatMessage({
        id: 'EAS_ALOG_COLUMN_USER',
        defaultMessage: 'Uživatel',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
  ];
}
