import { useFetch } from './fetch-hook';
import { Source, Params, ResultDto } from 'common/common-types';

export function useFetchResult<TYPE>(
  url: string,
  params: Params
): Source<TYPE> {
  const [result, loading, reset, setLoading] = useFetch<ResultDto<TYPE>>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  });

  return { items: [], count: 0, ...result, loading, setLoading, reset };
}
