import { useState, useContext } from 'react';
import { UserSettingsContext } from 'common/settings/user/user-settings-context';
import { TableSettings } from 'common/settings/user/user-settings-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { useUpdateEffect } from 'utils/update-effect';
import { TableSort } from '../table-types';
import { TableColumn } from '../table-types';

export function useTableSort<OBJECT>({
  tableId,
  version,
  columns,
  defaultSorts,
}: {
  tableId: string;
  version: number;
  columns: TableColumn<OBJECT>[];
  defaultSorts: TableSort[];
}) {
  const { getTableSettings, setTableSettings } = useContext(
    UserSettingsContext
  );

  let settings: TableSettings | undefined;
  let initSorts: TableSort[] = defaultSorts;

  if (tableId !== '') {
    settings = getTableSettings(tableId, version);

    if (settings?.sorts !== undefined) {
      initSorts = settings?.sorts;
    }
  }

  const [sorts, setSorts] = useState<TableSort[]>(initSorts);

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

  const resetSorts = useEventCallback(() => {
    setSorts(defaultSorts);
  });

  /**
   * Updates user settings.
   */
  useUpdateEffect(() => {
    const newSettings: TableSettings = {
      ...(settings ?? {}),
      sorts,
      version,
    };
    setTableSettings(tableId, newSettings);
  }, [sorts]);

  return { sorts, toggleSortColumn, resetSorts };
}
