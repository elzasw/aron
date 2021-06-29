import { useIntl } from 'react-intl';
import { TableCells } from 'composite/table/table-cells';
import { TableColumn } from 'composite/table/table-types';
import { TableFilterCells } from 'composite/table/table-filter-cells';
import { ExportTemplate } from '../export-types';
import {
  useExportDataProviders,
  useExportDesignProviders,
} from '../export-api';

export function useColumns(): TableColumn<ExportTemplate>[] {
  const intl = useIntl();

  return [
    {
      datakey: 'label',
      name: intl.formatMessage({
        id: 'EAS_EXPORT_TEMPLATES_COLUMN_LABEL',
        defaultMessage: 'Označení',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'dataProvider',
      sortkey: 'dataProvider.name',
      filterkey: 'dataProvider.id',
      name: intl.formatMessage({
        id: 'EAS_EXPORT_TEMPLATES_COLUMN_DATA_PROVIDER',
        defaultMessage: 'Zdroj dat',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useExportDataProviders
      ),
      valueMapper: TableCells.useSelectCellFactory(useExportDataProviders),
      sortable: true,
      filterable: true,
    },
    {
      datakey: 'designProvider',
      sortkey: 'designProvider.name',
      filterkey: 'designProvider.id',
      name: intl.formatMessage({
        id: 'EAS_EXPORT_TEMPLATES_COLUMN_DESIGN_PROVIDER',
        defaultMessage: 'Zdroj designu',
      }),
      width: 200,
      CellComponent: TableCells.TextCell,
      FilterComponent: TableFilterCells.useFilterSelectCellFactory(
        useExportDesignProviders
      ),
      valueMapper: TableCells.useSelectCellFactory(useExportDesignProviders),
      sortable: true,
      filterable: true,
    },
  ];
}
