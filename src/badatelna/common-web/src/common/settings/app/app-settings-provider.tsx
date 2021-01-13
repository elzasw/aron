import * as React from 'react';
import { AppSettingsProviderProps } from './app-settings-types';
import { AppSettingsContext } from './app-settings-context';
import { useAppSettings } from './app-settings-hook';

export function AppSettingsProvider({
  children,
  url,
  autoInit = true,
}: React.PropsWithChildren<AppSettingsProviderProps>) {
  const { context, settings } = useAppSettings(url);

  React.useEffect(() => {
    if (autoInit) {
      context.init();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <AppSettingsContext.Provider value={context}>
      {settings !== undefined && children}
    </AppSettingsContext.Provider>
  );
}
