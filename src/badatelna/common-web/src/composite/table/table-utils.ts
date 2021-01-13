import { TableColumn } from './table-types';

export function addColumnPrefix<T>(columns: TableColumn<T>[], prefix: string) {
  return columns.map(
    (column) =>
      ({
        ...column,
        datakey: `${prefix}.${column.datakey}`,
        sortkey: column.sortkey ? `${prefix}.${column.sortkey}` : undefined,
        filterkey: column.filterkey
          ? `${prefix}.${column.filterkey}`
          : undefined,
      } as TableColumn<T>)
  );
}
