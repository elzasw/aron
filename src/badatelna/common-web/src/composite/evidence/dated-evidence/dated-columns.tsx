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
        datakey: 'created',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_CREATED',
          defaultMessage: 'Vytvoření',
        }),
        width: 150,
        CellComponent: TableCells.DateTimeCell,
        sortable: true,
        filterable: true,
      },
      {
        datakey: 'updated',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_UPDATED',
          defaultMessage: 'Poslední úprava',
        }),
        width: 150,
        CellComponent: TableCells.DateTimeCell,
        sortable: true,
        filterable: true,
      },
    ],
    [options.columns, intl]
  );

  return columns;
}
