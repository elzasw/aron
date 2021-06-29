import * as React from 'react';
import { HistoryContext } from './history-context';
import { HistoryProviderProps } from './history-types';
import { useHistory } from './history-hook';

export function HistoryProvider({
  children,
  url,
}: React.PropsWithChildren<HistoryProviderProps>) {
  const { context } = useHistory(url);
  return (
    <HistoryContext.Provider value={context}>
      {children}
    </HistoryContext.Provider>
  );
}
