import { useCallback, useState, useContext, useEffect, useRef } from 'react';
import { useEventCallback } from 'utils/event-callback-hook';
import { TableContext } from '../table-context';
import { DialogHandle } from 'components/dialog/dialog-types';
import { NamedSettingsContext } from 'common/settings/named/named-settings-context';
import { NamedSettings } from 'common/settings/named/named-settings-types';
import { NamedTableSettings } from './named-settings-types';

export function useNamedSettingsMenu(tag: string) {
  const {
    getNamedSettings,
    saveNamedSettings,
    deleteNamedSettings,
  } = useContext(NamedSettingsContext);
  const {
    filtersState,
    setFiltersState,
    columnsState,
    setColumnsState,
    sorts,
    setSorts,
  } = useContext(TableContext);

  const [opened, setOpened] = useState<boolean>(false);
  const [savedItems, setSavedItems] = useState<NamedSettings[]>([]);
  const [selectedItem, setSelectedItem] = useState<string | null>(null);
  const confirmDeleteDialog = useRef<DialogHandle>(null);
  const createDialog = useRef<DialogHandle>(null);

  const selectSaved = useEventCallback((settings: NamedSettings) => {
    setSelectedItem(settings.id);

    const parsed: NamedTableSettings = JSON.parse(settings.settings);

    if (parsed.filtersState != null) {
      setFiltersState(parsed.filtersState);
    }

    if (parsed.columnsState != null) {
      setColumnsState(parsed.columnsState);
    }

    if (parsed.sorts != null) {
      setSorts(parsed.sorts);
    }
  });

  const load = useCallback(async () => {
    const settings = await getNamedSettings(tag);
    setSavedItems(settings);
  }, [getNamedSettings, tag]);

  const createSaved = useEventCallback(async (settings: NamedSettings) => {
    const parsed: NamedTableSettings = {
      filtersState,
      columnsState,
      sorts,
    };

    settings.settings = JSON.stringify(parsed);
    settings.tag = tag;

    settings = await saveNamedSettings(settings);
    setSelectedItem(settings.id);

    await load();
  });

  const deleteSaved = useEventCallback(async (id: string) => {
    await deleteNamedSettings(id);
    setSelectedItem(null);

    await load();
  });

  const openMenu = useCallback(() => {
    setOpened(true);
  }, []);

  const closeMenu = useCallback(() => {
    setOpened(false);
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return {
    opened,
    openMenu,
    closeMenu,
    savedItems,
    selectSaved,
    createSaved,
    deleteSaved,
    selectedItem,
    confirmDeleteDialog,
    createDialog,
  };
}
