import { useMemo } from 'react';
import { useIntl } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';

export function useColumns<OBJECT>(options: {
  columns?: TableColumn<OBJECT>[];
}) {
  const intl = useIntl();

  const columns: TableColumn<OBJECT>[] = useMemo(
    () => [
      ...(options.columns ?? []),
      {
        datakey: 'order',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_ORDER',
          defaultMessage: 'Pořadí',
        }),
        width: 100,
        CellComponent: TableCells.NumberCell,
        sortable: true,
        filterable: true,
      },
      {
        datakey: 'name',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_NAME',
          defaultMessage: 'Název',
        }),
        width: 200,
        CellComponent: TableCells.TextCell,
        sortable: true,
        filterable: true,
      },
      {
        datakey: 'active',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_ACTIVE',
          defaultMessage: 'Aktivní',
        }),
        width: 100,
        CellComponent: TableCells.BooleanCell,
        sortable: true,
        filterable: true,
      },
      {
        datakey: 'validFrom',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_VALID_FROM',
          defaultMessage: 'Platnost od',
        }),
        width: 150,
        CellComponent: TableCells.DateTimeCell,
        sortable: true,
        filterable: true,
      },
      {
        datakey: 'validTo',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_VALID_TO',
          defaultMessage: 'Platnost do',
        }),
        width: 150,
        CellComponent: TableCells.DateTimeCell,
        sortable: true,
        filterable: true,
      },
      {
        datakey: 'code',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_CODE',
          defaultMessage: 'Kód',
        }),
        width: 100,
        CellComponent: TableCells.TextCell,
        sortable: true,
        filterable: true,
      },
    ],
    [options.columns, intl]
  );

  return columns;
}
