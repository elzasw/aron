import { useState, useRef, useMemo, useContext } from 'react';
import { unstable_batchedUpdates } from 'react-dom';
import { useIntl } from 'react-intl';
import {
  Params,
  ScrollableSource,
  Source,
  ResultDto,
} from 'common/common-types';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { abortableFetch, AbortableFetch } from './abortable-fetch';
import { useEventCallback } from './event-callback-hook';
import { useSerializeCall } from './serialize-call-hook';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';

export type Url = string | (() => string);

export function useScrollableSource<TYPE>({
  url,
  params,
  listItems = defaultListItems,
}: {
  url: Url;
  params?: Params;
  listItems?: (url: string, params: Params) => AbortableFetch;
}) {
  const fetch = useRef<AbortableFetch | null>(null);
  const paramsRef = useRef<Params>({ size: 30 });
  const dataRef = useRef<ResultDto<TYPE>>({
    items: [],
    count: 0,
  });
  const isDataValidRef = useRef<boolean>(false);
  const hasNextPageRef = useRef<boolean>(true);

  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<ResultDto<TYPE>>(dataRef.current);

  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);

  const loadMoreUnsafe = useEventCallback(async () => {
    try {
      if (isDataValidRef.current && !hasNextPageRef.current) {
        // skip
        return;
      }

      setLoading(true);

      const resolvedUrl = typeof url === 'function' ? url() : url;
      const combinedParams: Params = {
        ...params,
        ...(paramsRef.current ?? {}),
        searchAfter: isDataValidRef.current
          ? dataRef.current.searchAfter
          : undefined,
      };

      fetch.current = listItems(resolvedUrl, combinedParams);

      const data: Source<TYPE> = await fetch.current.json();

      if (!isDataValidRef.current) {
        dataRef.current = data;
      } else {
        dataRef.current = {
          ...data,
          items: [...dataRef.current.items, ...data.items],
        };
      }
      isDataValidRef.current = true;
      hasNextPageRef.current = data.count > dataRef.current.items.length;

      unstable_batchedUpdates(() => {
        setResult(dataRef.current);
        setLoading(false);
      });

      fetch.current = null;
    } catch (err) {
      if (err.name !== 'AbortError') {
        isDataValidRef.current = true;
        hasNextPageRef.current = false;

        const message = intl.formatMessage(
          {
            id: 'EAS_SCROLLABLE_SOURCE_MSG_ERROR',
            defaultMessage: 'Chyba načtení dat: {detail}',
          },
          { detail: err.message }
        );

        setLoading(false);
        showSnackbar(message, SnackbarVariant.ERROR);
      }
    }

    return;
  });

  const [loadMore] = useSerializeCall(loadMoreUnsafe);

  const source: ScrollableSource<TYPE> = useMemo(
    () => ({
      ...result,
      loading,
      setLoading,
      loadMore,
      setParams: (p) => (paramsRef.current = p),
      getParams: () => paramsRef.current,
      reset: () => {
        unstable_batchedUpdates(() => {
          fetch.current?.abort();
          hasNextPageRef.current = true;
          isDataValidRef.current = false;

          setLoading(false);
          setResult({
            items: [],
            count: 0,
          });
        });
      },
      hasNextPage: () => hasNextPageRef.current,
      isDataValid: () => isDataValidRef.current,
    }),
    [result, loadMore, loading]
  );

  return source;
}

export function defaultListItems(url: string, params: Params) {
  return abortableFetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params),
  });
}

export function listItemsFactory<TYPE>({
  listItemsMethod = defaultListItems,
  postProcess = async (data) => data,
}: {
  listItemsMethod?: (url: string, params: Params) => AbortableFetch;
  postProcess?: (data: Source<TYPE>) => Promise<Source<TYPE>>;
}) {
  return function listItems(url: string, params: Params) {
    const fetch = listItemsMethod(url, params);

    const augmented: AbortableFetch = {
      response: fetch.response,
      abort: fetch.abort,
      json: async () => {
        const data = await fetch.json();
        const processedData = postProcess(data);
        return processedData;
      },
      text: async () => {
        throw new Error('Unsupported operation');
      },
      raw: async () => {
        throw new Error('Unsupported operation');
      },
      none: async () => {
        throw new Error('Unsupported operation');
      },
    };

    return augmented;
  };
}
