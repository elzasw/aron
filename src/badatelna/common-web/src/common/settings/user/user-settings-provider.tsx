import * as React from 'react';
import { UserSettingsProviderProps } from './user-settings-types';
import { UserSettingsContext } from './user-settings-context';
import { useUserSettings } from './user-settings-hook';

export function UserSettingsProvider({
  children,
  url,
  autoInit = true,
}: React.PropsWithChildren<UserSettingsProviderProps>) {
  const { context, settings } = useUserSettings(url);

  React.useEffect(() => {
    if (autoInit) {
      context.init();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <UserSettingsContext.Provider value={context}>
      {settings !== undefined && children}
    </UserSettingsContext.Provider>
  );
}
