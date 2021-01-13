import { useMemo, useContext, useState } from 'react';
import { AppSettingsContext } from './app-settings-context';
import { useEventCallback } from 'utils/event-callback-hook';
import { useIntl } from 'react-intl';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { fetchSettings, updateSettings } from './app-settings-api';
import { AppSettings } from './app-settings-types';

export function useAppSettings(url: string) {
  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);
  const [loading, setLoading] = useState(false);
  const [settings, setSettings] = useState<AppSettings | undefined>(undefined);

  const load = useEventCallback(async () => {
    try {
      const response = await fetchSettings(url).response;

      let settings: AppSettings;

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
            id: 'EAS_APP_SETTINGS_MSG_LOAD_ERROR',
            defaultMessage: 'Chyba načítaní aplikačních nastavení: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const save = useEventCallback(async (settings: AppSettings) => {
    try {
      await updateSettings(url, settings).none();

      setLoading(false);
    } catch (err) {
      setLoading(false);
      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_APP_SETTINGS_MSG_SAVE_ERROR',
            defaultMessage: 'Chyba uložení aplikačních nastavení: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const update = useEventCallback(async (settings: AppSettings) => {
    setSettings(settings);
    await save(settings);
  });

  const init = useEventCallback(async () => {
    const settings = await load();

    if (settings !== undefined) {
      setSettings(settings);
    }
  });

  const context: AppSettingsContext = useMemo(
    () => ({
      init,
      update,
      settings: settings ?? {},
    }),
    [init, update, settings]
  );

  return { context, loading, settings };
}
