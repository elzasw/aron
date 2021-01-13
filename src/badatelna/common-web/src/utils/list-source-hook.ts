import { noop } from 'lodash';
import { useFetch } from './fetch-hook';
import { Params, ListSource } from 'common/common-types';
import { useMemo } from 'react';

export function useListSource<TYPE>({
  url,
  params,
}: {
  url: string;
  params?: Params;
}): ListSource<TYPE> {
  const [result, loading, reset] = useFetch<TYPE[]>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: params !== undefined ? JSON.stringify(params) : undefined,
  });

  const source: ListSource<TYPE> = useMemo(
    () => ({
      items: result ?? [],
      loading,
      reset,
    }),
    [loading, reset, result]
  );

  return source;
}

export function useStaticListSource<TYPE>(items: TYPE[]): ListSource<TYPE> {
  return {
    loading: false,
    reset: noop,
    items,
  };
}
