import { useContext } from 'react';
import { HistoryContext } from 'common/history/history-context';
import { useListSource } from 'utils/list-source-hook';
import { History } from './history-types';

export function useHistorySource(id: string) {
  const { url } = useContext(HistoryContext);

  return useListSource<History>({
    url: `${url}/${id}/full`,
  });
}
