import React from 'react';
import { FormattedMessage } from 'react-intl';
import {
  ReportTemplate,
  ListSource,
  ReportProvider,
} from 'common/common-types';
import { TableCells } from 'composite/table/table-cells';
import { TableColumn } from 'composite/table/table-types';
import { TableFilterCells } from 'composite/table/table-filter-cells';

export function useColumns(
  useReportProviders: () => ListSource<ReportProvider>
): TableColumn<ReportTemplate>[] {
  return [
    {
      datakey: 'label',
      name: (
        <FormattedMessage
          id="EAS_REPORT_TEMPLATES_COLUMN_LABEL"
          defaultMessage="Označení"
        />
      ),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'provider',
      sortkey: 'provider.name',
      filterkey: 'provider.id',
      name: (
        <FormattedMessage
          id="EAS_REPORT_TEMPLATES_COLUMN_PROVIDER"
          defaultMessage="Poskytovatel dat"
        />
      ),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useReportProviders
      ),
      valueMapper: TableCells.useSelectCellFactory(useReportProviders),
      sortable: true,
      filterable: true,
    },
  ];
}
