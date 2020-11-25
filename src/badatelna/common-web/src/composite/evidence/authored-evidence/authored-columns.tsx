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
        datakey: 'createdBy',
        sortkey: 'createdBy.name',
        filterkey: 'createdBy.name',
        name: (
          <FormattedMessage
            id="EAS_EVIDENCE_COLUMN_CREATED_BY"
            defaultMessage="Autor"
          />
        ),
        width: 150,
        CellComponent: TableCells.TextCell,
        sortable: true,
        filterable: true,
        valueMapper: TableCells.dictionaryColumnMapper,
      },
      {
        datakey: 'updatedBy',
        sortkey: 'updatedBy.name',
        filterkey: 'updatedBy.name',
        name: (
          <FormattedMessage
            id="EAS_EVIDENCE_COLUMN_UPDATED_BY"
            defaultMessage="Autor úpravy"
          />
        ),
        width: 150,
        CellComponent: TableCells.TextCell,
        sortable: true,
        filterable: true,
        valueMapper: TableCells.dictionaryColumnMapper,
      },
    ],
    [options.columns]
  );

  return columns;
}
