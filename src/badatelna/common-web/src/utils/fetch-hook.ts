import { useState, useEffect, useContext } from 'react';
import { unstable_batchedUpdates } from 'react-dom';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { abortableFetch, AbortableFetch } from './abortable-fetch';
import { useEventCallback } from './event-callback-hook';
import { useIntl } from 'react-intl';

export function useFetch<RESULT>(request: RequestInfo, opts?: RequestInit) {
  const [result, setResult] = useState<RESULT>();
  const [loading, setLoading] = useState<boolean>(false);
  const [counter, setCounter] = useState(0);

  const key = JSON.stringify({ request, opts, counter });

  const { showSnackbar } = useContext(SnackbarContext);
  const intl = useIntl();

  useEffect(() => {
    let fetch: AbortableFetch;

    async function load() {
      try {
        setLoading(true);
        fetch = abortableFetch(request, opts);

        const data = await fetch.json();

        unstable_batchedUpdates(() => {
          setResult(data);
          setLoading(false);
        });
      } catch (err) {
        if (err.name !== 'AbortError') {
          setLoading(false);

          const message = intl.formatMessage(
            {
              id: 'EAS_FETCH_MSG_ERROR',
              defaultMessage: 'Chyba načtení dat: {detail}',
            },
            { detail: err.message }
          );

          showSnackbar(message, SnackbarVariant.ERROR);
          throw err;
        }
      }
    }

    load();

    return () => {
      fetch.abort();
    };
    // run effect only if input params change
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  const reset = useEventCallback(() => {
    setCounter((counter) => ++counter);
  });

  return [result, loading, reset] as [RESULT | undefined, boolean, () => void];
}
