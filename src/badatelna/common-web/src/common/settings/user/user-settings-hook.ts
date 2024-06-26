import { useMemo, useContext, useState, useCallback } from 'react';
import { get, set } from 'lodash';
import { UserSettingsContext } from './user-settings-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { useIntl } from 'react-intl';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import {
  fetchSettings,
  updateSettings,
  clearSettings,
} from './user-settings-api';
import {
  UserSettings,
  TableSettings,
  CustomSettings,
} from './user-settings-types';

export function useUserSettings(url: string) {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);
  const [loading, setLoading] = useState(false);
  const [settings, setSettings] = useState<UserSettings | undefined>(undefined);

  const load = useEventCallback(async () => {
    try {
      const response = await fetchSettings(url).response;

      let settings: UserSettings;

      try {
        const json = await response.json();
        settings = json;
      } catch {
        settings = {};
      }

      setLoading(false);

      return settings;
    } catch (err) {
      setLoading(false);
      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_USER_SETTINGS_MSG_LOAD_ERROR',
            defaultMessage: 'Chyba načítaní nastavení: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const save = useEventCallback(async (settings: UserSettings) => {
    try {
      await updateSettings(url, settings).none();

      setLoading(false);
    } catch (err) {
      setLoading(false);
      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_USER_SETTINGS_MSG_SAVE_ERROR',
            defaultMessage: 'Chyba uložení nastavení: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const clear = useEventCallback(async () => {
    try {
      await clearSettings(url).none();

      await init();

      setLoading(false);
    } catch (err) {
      setLoading(false);
      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_USER_SETTINGS_MSG_CLEAR_ERROR',
            defaultMessage: 'Chyba mazání nastavení: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const init = useEventCallback(async () => {
    const settings = await load();

    if (settings !== undefined) {
      setSettings(settings);
    }
  });

  /**
   * Cant use event callback because its called from render function
   */
  const getTableSettings = useCallback(
    (tableId: string, version: number) => {
      const tableSettings = settings?.tables?.[tableId];

      if (tableSettings !== undefined && version === tableSettings.version) {
        return tableSettings;
      }

      return undefined;
    },
    [settings]
  );

  const setTableSettings = useEventCallback(
    (tableId: string, tableSettings: TableSettings) => {
      if (settings === undefined) {
        return;
      }

      const newUserSettings = {
        ...settings,
        tables: { ...settings.tables, [tableId]: tableSettings },
      };
      setSettings(newUserSettings);

      save(newUserSettings);
    }
  );

  const getCustomSettings = useCallback(
    (key: string, version: number) => {
      const customSettings = get(settings, key);

      if (customSettings !== undefined && version === customSettings.version) {
        return customSettings as CustomSettings;
      }

      return undefined;
    },
    [settings]
  );

  const setCustomSettings = useEventCallback(
    (key: string, customSettings: CustomSettings) => {
      if (settings === undefined) {
        return;
      }

      const newUserSettings = {
        ...settings,
      };
      set(newUserSettings, key, customSettings);

      setSettings(newUserSettings);

      save(newUserSettings);
    }
  );

  const context: UserSettingsContext = useMemo(
    () => ({
      init,
      getTableSettings,
      setTableSettings,
      getCustomSettings,
      setCustomSettings,
      clear,
    }),
    [
      init,
      clear,
      getTableSettings,
      setTableSettings,
      getCustomSettings,
      setCustomSettings,
    ]
  );

  return { context, loading, settings };
}
