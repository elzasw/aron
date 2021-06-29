import { useMemo } from 'react';
import { noop } from 'lodash';
import { useFetch } from './fetch-hook';
import { Params, ListSource, DomainObject } from 'common/common-types';
import { useEventCallback } from './event-callback-hook';
import { defaultGetItem as fetchItem } from 'utils/crud-source-hook';

export function useListSource<TYPE extends DomainObject>({
  url,
  apiUrl,
  params,
  method = 'POST',
}: {
  url: string;
  apiUrl?: string;
  params?: Params;
  method?: 'POST' | 'GET';
}): ListSource<TYPE> {
  const [result, loading, reset] = useFetch<TYPE[]>(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: params !== undefined ? JSON.stringify(params) : undefined,
  });

  const loadDetail = useEventCallback(async (item: TYPE) => {
    if (apiUrl !== undefined) {
      const fetch = fetchItem(apiUrl, item.id);
      const detail = await fetch.json();
      return detail as TYPE;
    }
    return Promise.resolve(item);
  });

  const source: ListSource<TYPE> = useMemo(
    () => ({
      items: result ?? [],
      loading,
      reset,
      loadDetail,
    }),
    [loading, reset, result, loadDetail]
  );

  return source;
}

export function useStaticListSource<TYPE>(items: TYPE[]): ListSource<TYPE> {
  return {
    loading: false,
    reset: noop,
    loadDetail: identityPromise,
    items,
  };
}

function identityPromise<T>(item: T) {
  return Promise.resolve(item);
}
