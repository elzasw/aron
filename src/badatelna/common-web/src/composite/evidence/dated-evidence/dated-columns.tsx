import React, { useMemo } from 'react';
import { FormattedMessage } from 'react-intl';
import { TableColumn } from 'composite/table/table-types';
import { TableCells } from 'composite/table/table-cells';

export function useColumns<OBJECT>(options: {
  columns?: TableColumn<OBJECT>[];
}) {
  const columns: TableColumn<OBJECT>[] = useMemo(
    () => [
      ...(options.columns ?? []),
      {
        datakey: 'created',
        name: (
          <FormattedMessage
            id="EAS_EVIDENCE_COLUMN_CREATED"
            defaultMessage="Vytvoření"
          />
        ),
        width: 150,
        CellComponent: TableCells.DateTimeCell,
        sortable: true,
        filterable: true,
      },
      {
        datakey: 'updated',
        name: (
          <FormattedMessage
            id="EAS_EVIDENCE_COLUMN_UPDATED"
            defaultMessage="Poslední úprava"
          />
        ),
        width: 150,
        CellComponent: TableCells.DateTimeCell,
        sortable: true,
        filterable: true,
      },
    ],
    [options.columns]
  );

  return columns;
}
