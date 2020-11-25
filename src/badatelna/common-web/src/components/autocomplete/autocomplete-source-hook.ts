import { useScrollableSource } from 'utils/scrollable-source-hook';
import { useMemo, useRef } from 'react';
import { AutocompleteSource } from './autocomplete-types';
import { useEventCallback } from 'utils/event-callback-hook';

export function useAutocompleteSource<ITEM>(
  url: string
): AutocompleteSource<ITEM> {
  const query = useRef<string>('');

  const setSearchQuery = useEventCallback((q: string) => {
    query.current = q;
  });

  const getUrl = useEventCallback(() => {
    return `${url}?query=${query.current}`;
  });

  const result = useScrollableSource<ITEM>(getUrl);

  const source = useMemo(
    () => ({
      ...result,
      setSearchQuery,
    }),
    [result, setSearchQuery]
  );
  return source;
}
