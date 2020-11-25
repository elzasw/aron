import { useState } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import { TableSort } from '../table-types';
import { TableColumn } from '../table-types';

export function useTableSort<OBJECT>({
  columns,
  defaultSorts,
}: {
  columns: TableColumn<OBJECT>[];
  defaultSorts: TableSort[];
}) {
  const [sorts, setSorts] = useState<TableSort[]>(defaultSorts);

  const toggleSortColumn = useEventCallback((datakey: string) => {
    const sortIndex = sorts.findIndex((sort) => sort.datakey === datakey);

    if (sortIndex === -1) {
      const column = columns.find((column) => column.datakey === datakey);

      setSorts([
        ...sorts,
        {
          datakey,
          field: column?.sortkey ?? datakey,
          order: 'ASC',
          type: column?.sortType ?? 'FIELD',
        },
      ]);
    } else {
      const sort = sorts[sortIndex];

      if (sort.order === 'ASC') {
        setSorts([
          ...sorts.slice(0, sortIndex),
          {
            ...sort,
            order: 'DESC',
          },
          ...sorts.slice(sortIndex + 1, undefined),
        ]);
      } else {
        setSorts([
          ...sorts.slice(0, sortIndex),
          ...sorts.slice(sortIndex + 1, undefined),
        ]);
      }
    }
  });

  return { sorts, toggleSortColumn };
}
