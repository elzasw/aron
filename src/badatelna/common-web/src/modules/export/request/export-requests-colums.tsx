import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { ExportRequest } from '../export-types';
import { useExportRequestStates, useExportTypes } from '../export-api';

export function useColumns(): TableColumn<ExportRequest>[] {
  const intl = useIntl();
  return [
    {
      datakey: 'state',
      sortkey: 'state.name',
      filterkey: 'state.id',
      name: intl.formatMessage({
        id: 'EAS_EXPORT_REQUESTS_COLUMN_STATE',
        defaultMessage: 'Stav',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useExportRequestStates
      ),
      valueMapper: TableCells.useSelectCellFactory(useExportRequestStates),
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'type',
      sortkey: 'type.name',
      filterkey: 'type.id',
      name: intl.formatMessage({
        id: 'EAS_EXPORT_REQUESTS_COLUMN_TYPE',
        defaultMessage: 'Typ výstupu',
      }),
      width: 150,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useExportTypes
      ),
      valueMapper: TableCells.useSelectCellFactory(useExportTypes),
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'processingStart',
      name: intl.formatMessage({
        id: 'EAS_EXPORT_REQUESTS_COLUMN_PROCESSING_START',
        defaultMessage: 'Začátek zpracování',
      }),
      width: 200,
      CellComponent: TableCells.DateTimeCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'processingEnd',
      name: intl.formatMessage({
        id: 'EAS_EXPORT_REQUESTS_COLUMN_PROCESSING_END',
        defaultMessage: 'Konec zpracování',
      }),
      width: 200,
      CellComponent: TableCells.DateTimeCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'systemRequest',
      name: intl.formatMessage({
        id: 'EAS_EXPORT_REQUESTS_COLUMN_SYSTEM_REQUEST',
        defaultMessage: 'Systémový',
      }),
      width: 120,
      CellComponent: TableCells.BooleanCell,
      sortable: true,
      filterable: true,
    },
  ];
}
