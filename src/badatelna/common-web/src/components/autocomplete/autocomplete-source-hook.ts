import { useScrollableSource } from 'utils/scrollable-source-hook';
import { useMemo, useRef } from 'react';
import { AutocompleteSource } from './autocomplete-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { defaultGetItem as fetchItem } from 'utils/crud-source-hook';
import { Params } from 'common/common-types';

export function useAutocompleteSource<ITEM extends { id: string }>({
  url,
  apiUrl,
  params,
}: {
  url: string;
  apiUrl?: string;
  params?: Params;
}): AutocompleteSource<ITEM> {
  const query = useRef<string>('');

  const setSearchQuery = useEventCallback((q: string) => {
    query.current = q;
  });

  const getUrl = useEventCallback(() => {
    return `${url}?query=${encodeURIComponent(query.current)}`;
  });

  const loadDetail = useEventCallback(async (item: ITEM) => {
    if (apiUrl !== undefined) {
      const fetch = fetchItem(apiUrl, item.id);
      const detail = await fetch.json();
      return detail as ITEM;
    }
    return Promise.resolve(item);
  });

  const result = useScrollableSource<ITEM>({ url: getUrl, params });

  const source = useMemo(
    () => ({
      ...result,
      setSearchQuery,
      loadDetail,
    }),
    [result, setSearchQuery, loadDetail]
  );
  return source;
}
