import { useState, useContext } from 'react';
import { useDebouncedCallback } from 'use-debounce/lib';
import { TableSettings } from 'common/settings/user/user-settings-types';
import { useUpdateEffect } from 'utils/update-effect';
import { UserSettingsContext } from 'common/settings/user/user-settings-context';

export function useTableSearch({
  tableId,
  version,
}: {
  tableId: string;
  version: number;
}) {
  const { getTableSettings, setTableSettings } = useContext(
    UserSettingsContext
  );

  let settings: TableSettings | undefined;
  let initQuery = '';

  if (tableId !== '') {
    settings = getTableSettings(tableId, version);

    if (settings?.searchQuery !== undefined) {
      initQuery = settings?.searchQuery;
    }
  }

  const [searchQuery, setSearchQueryInternal] = useState(initQuery);

  /**
   * Debounces set search query.
   */
  const [setSearchQuery] = useDebouncedCallback(setSearchQueryInternal, 500);

  /**
   * Updates user settings.
   */
  useUpdateEffect(() => {
    const newSettings: TableSettings = {
      ...(settings ?? {}),
      searchQuery,
      version,
    };
    setTableSettings(tableId, newSettings);
  }, [searchQuery]);

  return { searchQuery, setSearchQuery };
}
