import * as React from 'react';
import { NamedSettingsProviderProps } from './named-settings-types';
import { useNamedSettings } from './named-settings-hook';
import { NamedSettingsContext } from './named-settings-context';

export function NamedSettingsProvider({
  children,
  url,
  defaultTableNamedSettings,
}: React.PropsWithChildren<NamedSettingsProviderProps>) {
  const { context } = useNamedSettings(url, defaultTableNamedSettings);

  return (
    <NamedSettingsContext.Provider value={context}>
      {children}
    </NamedSettingsContext.Provider>
  );
}
