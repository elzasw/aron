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
        datakey: 'createdBy',
        displaykey: 'createdBy.name',
        sortkey: 'createdBy.name',
        filterkey: 'createdBy.name',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_CREATED_BY',
          defaultMessage: 'Autor',
        }),
        width: 150,
        CellComponent: TableCells.TextCell,
        sortable: true,
        filterable: true,
      },
      {
        datakey: 'updatedBy',
        displaykey: 'updatedBy.name',
        sortkey: 'updatedBy.name',
        filterkey: 'updatedBy.name',
        name: intl.formatMessage({
          id: 'EAS_EVIDENCE_COLUMN_UPDATED_BY',
          defaultMessage: 'Autor úpravy',
        }),
        width: 150,
        CellComponent: TableCells.TextCell,
        sortable: true,
        filterable: true,
      },
    ],
    [options.columns, intl]
  );

  return columns;
}
