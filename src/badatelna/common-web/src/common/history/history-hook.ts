import { useMemo } from 'react';
import { HistoryContext } from './history-context';

export function useHistory(url: string) {
  const context: HistoryContext = useMemo(
    () => ({
      url,
    }),
    [url]
  );

  return { context };
}
