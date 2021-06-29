import { useMemo, useContext, useState } from 'react';
import { useIntl } from 'react-intl';
import { useEventCallback } from 'utils/event-callback-hook';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import {
  fetchSettingsByTag,
  createSettings,
  deleteSettings,
} from './named-settings-api';
import { NamedSettings } from './named-settings-types';
import { NamedSettingsContext } from './named-settings-context';

export function useNamedSettings(
  url: string,
  defaultTableNamedSettings = false
) {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);
  const [loading, setLoading] = useState(false);

  const getNamedSettings = useEventCallback(async (tag: string) => {
    try {
      const response = await fetchSettingsByTag(url, tag).response;

      let settings: NamedSettings[];

      try {
        const json = await response.json();
        settings = json;
      } catch {
        settings = [];
      }

      setLoading(false);

      return settings;
    } catch (err) {
      setLoading(false);
      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_NAMED_SETTINGS_MSG_LOAD_ERROR',
            defaultMessage: 'Chyba načtení nastavení: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }

      return [];
    }
  });

  const saveNamedSettings = useEventCallback(
    async (settings: NamedSettings) => {
      try {
        const result: NamedSettings = await createSettings(
          url,
          settings
        ).json();

        setLoading(false);

        return result;
      } catch (err) {
        setLoading(false);
        if (err.name !== 'AbortError') {
          const message = intl.formatMessage(
            {
              id: 'EAS_NAMED_SETTINGS_MSG_SAVE_ERROR',
              defaultMessage: 'Chyba uložení nastavení: {detail}',
            },
            { detail: err.message }
          );

          showSnackbar(message, SnackbarVariant.ERROR);

          throw err;
        }

        throw err;
      }
    }
  );

  const deleteNamedSettings = useEventCallback(async (id: string) => {
    try {
      await deleteSettings(url, id).none();

      setLoading(false);
    } catch (err) {
      setLoading(false);
      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_NAMED_SETTINGS_MSG_CLEAR_ERROR',
            defaultMessage: 'Chyba mazání nastavení: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const context: NamedSettingsContext = useMemo(
    () => ({
      getNamedSettings,
      saveNamedSettings,
      deleteNamedSettings,
      defaultTableNamedSettings,
    }),
    [
      getNamedSettings,
      saveNamedSettings,
      deleteNamedSettings,
      defaultTableNamedSettings,
    ]
  );

  return { context, loading };
}
