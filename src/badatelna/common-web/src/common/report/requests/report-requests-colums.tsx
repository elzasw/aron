import React from 'react';
import { FormattedMessage } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { ReportRequest } from 'common/common-types';
import { TableCells } from 'composite/table/table-cells';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { useReportRequestStates, useReportTypes } from './report-requests-api';

export function useColumns(): TableColumn<ReportRequest>[] {
  return [
    {
      datakey: 'state',
      sortkey: 'state.name',
      filterkey: 'state.id',
      name: (
        <FormattedMessage
          id="EAS_REPORT_REQUESTS_COLUMN_STATE"
          defaultMessage="Stav"
        />
      ),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useReportRequestStates
      ),
      valueMapper: TableCells.useSelectCellFactory(useReportRequestStates),
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'type',
      sortkey: 'type.name',
      filterkey: 'type.id',
      name: (
        <FormattedMessage
          id="EAS_REPORT_REQUESTS_COLUMN_TYPE"
          defaultMessage="Typ výstupu"
        />
      ),
      width: 150,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useReportTypes
      ),
      valueMapper: TableCells.useSelectCellFactory(useReportTypes),
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'processingStart',
      name: (
        <FormattedMessage
          id="EAS_REPORT_REQUESTS_COLUMN_PROCESSING_START"
          defaultMessage="Začátek zpracování"
        />
      ),
      width: 200,
      CellComponent: TableCells.DateTimeCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'processingEnd',
      name: (
        <FormattedMessage
          id="EAS_REPORT_REQUESTS_COLUMN_PROCESSING_END"
          defaultMessage="Konec zpracování"
        />
      ),
      width: 200,
      CellComponent: TableCells.DateTimeCell,
      sortable: true,
      filterable: true,
    },
  ];
}
