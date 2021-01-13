import * as React from 'react';
import { HelpContext } from './help-context';
import { HelpProviderProps } from './help-types';

export function HelpProvider({
  children,
  formContextHelpType,
}: React.PropsWithChildren<HelpProviderProps>) {
  return (
    <HelpContext.Provider value={{ formContextHelpType }}>
      {children}
    </HelpContext.Provider>
  );
}
