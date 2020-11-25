import { useState, useRef, useContext } from 'react';
import { unstable_batchedUpdates } from 'react-dom';
import { useIntl } from 'react-intl';
import { CrudSource, DomainObject } from 'common/common-types';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { abortableFetch, AbortableFetch } from './abortable-fetch';
import { useEventCallback } from './event-callback-hook';

export function useCrudSource<TYPE extends DomainObject>(
  url: string
): CrudSource<TYPE> {
  const [loading, setLoading] = useState<boolean>(false);
  const [data, setData] = useState<TYPE | null>(null);
  const fetch = useRef<AbortableFetch | null>(null);

  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);

  const get = useEventCallback(async (id: string) => {
    try {
      setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = fetchItem(url, id);

      const data: TYPE = await fetch.current.json();
      unstable_batchedUpdates(() => {
        setData(data);
        setLoading(false);
      });
      return data;
    } catch (err) {
      setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_CRUD_SOURCE_MSG_LOAD_ERROR',
            defaultMessage: 'Chyba načtení dat: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
      return undefined;
    }
  });

  const create = useEventCallback(async (item: TYPE) => {
    try {
      setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = createItem(url, item);

      const data: TYPE = await fetch.current.json();

      const message = intl.formatMessage({
        id: 'EAS_CRUD_SOURCE_MSG_CREATE_SUCCESS',
        defaultMessage: 'Záznam byl úspěšně vytvořen.',
      });

      unstable_batchedUpdates(() => {
        showSnackbar(message, SnackbarVariant.SUCCESS);
        setData(data);
        setLoading(false);
      });
      return data;
    } catch (err) {
      setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_CRUD_SOURCE_MSG_CREATE_ERROR',
            defaultMessage: 'Chyba uložení dat: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
      return undefined;
    }
  });

  const update = useEventCallback(async (item: TYPE, prevItem: TYPE) => {
    try {
      setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = updateItem(url, item, prevItem);

      const data: TYPE = await fetch.current.json();

      const message = intl.formatMessage({
        id: 'EAS_CRUD_SOURCE_MSG_UPDATE_SUCCESS',
        defaultMessage: 'Záznam byl úspěšně upraven.',
      });

      unstable_batchedUpdates(() => {
        showSnackbar(message, SnackbarVariant.SUCCESS);
        setData(data);
        setLoading(false);
      });
      return data;
    } catch (err) {
      setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_CRUD_SOURCE_MSG_UPDATE_ERROR',
            defaultMessage: 'Chyba uložení dat: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
      return undefined;
    }
  });

  const del = useEventCallback(async (id: string) => {
    try {
      setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = deleteItem(url, id);

      await fetch.current.none();

      const message = intl.formatMessage({
        id: 'EAS_CRUD_SOURCE_MSG_DELETE_SUCCESS',
        defaultMessage: 'Záznam byl úspěšně smazán.',
      });

      unstable_batchedUpdates(() => {
        showSnackbar(message, SnackbarVariant.SUCCESS);
        setData(null);
        setLoading(false);
      });
    } catch (err) {
      setLoading(false);

      if (err.name !== 'AbortError') {
        const message = intl.formatMessage(
          {
            id: 'EAS_CRUD_SOURCE_MSG_DELETE_ERROR',
            defaultMessage: 'Chyba mazání dat: {detail}',
          },
          { detail: err.message }
        );

        showSnackbar(message, SnackbarVariant.ERROR);

        throw err;
      }
    }
  });

  const refresh = useEventCallback(async () => {
    if (data !== null) {
      return get(data.id);
    }
  });

  const reset = useEventCallback(() => {
    if (fetch.current !== null) {
      fetch.current.abort();
      fetch.current = null;
    }

    setData(null);
  });

  return {
    url,
    data,
    loading,
    setLoading,
    get,
    create,
    update,
    del,
    refresh,
    reset,
  };
}

/**
 * Fetches data for single item from API.
 *
 * @param api API url
 */
export function fetchItem(api: string, itemId: string) {
  return abortableFetch(`${api}/${itemId}`, {
    method: 'GET',
    headers: new Headers({ 'Content-Type': 'application/json' }),
  });
}

/**
 * Calls create API method.
 *
 * @param api API endpoint
 * @param item Object to save
 */
export function createItem<TYPE>(api: string, item: TYPE) {
  return abortableFetch(api, {
    method: 'POST',
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    body: JSON.stringify(item),
  });
}

/**
 * Calls update API method.
 *
 *
 * @param api API endpoint
 * @param initialItem Initial data from last saved instance
 * @param item Object to save
 */
export function updateItem<TYPE extends DomainObject>(
  api: string,
  item: TYPE,
  _initialItem: TYPE
) {
  return abortableFetch(`${api}/${item.id}`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'PUT',
    body: JSON.stringify(item),
  });
}

/**
 * Calls delete API method.
 *
 * @param api API endpoint
 * @param itemId Id of object
 */
export function deleteItem(api: string, itemId: string) {
  return abortableFetch(`${api}/${itemId}`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'DELETE',
  });
}
