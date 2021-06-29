import { useState, useRef, useContext } from 'react';
import { unstable_batchedUpdates } from 'react-dom';
import { useIntl } from 'react-intl';
import { CrudSource, DomainObject, CrudSourceProps } from 'common/common-types';
import { SnackbarContext } from 'composite/snackbar/snackbar-context';
import { SnackbarVariant } from 'composite/snackbar/snackbar-types';
import { abortableFetch, AbortableFetch } from './abortable-fetch';
import { useEventCallback } from './event-callback-hook';

export function useCrudSource<TYPE extends DomainObject>({
  url,
  getItem = defaultGetItem,
  createItem = defaultCreateItem,
  updateItem = defaultUpdateItem,
  deleteItem = defaultDeleteItem,
  handleGetError,
  handleCreateError,
  handleUpdateError,
  handleDeleteError,
  getMessages,
  createMessages,
  updateMessages,
  delMessages,
}: CrudSourceProps<TYPE>): CrudSource<TYPE> {
  const [loading, setLoading] = useState<boolean>(false);
  const [data, setData] = useState<TYPE | null>(null);
  const fetch = useRef<AbortableFetch | null>(null);

  const intl = useIntl();
  const { showSnackbar } = useContext(SnackbarContext);

  const handleGet = useEventCallback(async (id: string) => {
    try {
      setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = getItem(url, id);

      const data: TYPE = await fetch.current.json();
      unstable_batchedUpdates(() => {
        setData(data);
        setLoading(false);
      });
      return data;
    } catch (err) {
      setLoading(false);

      if (err.name !== 'AbortError') {
        if (handleGetError !== undefined) {
          handleGetError(err);
        } else {
          const message =
            getMessages?.errorMessage ??
            intl.formatMessage({
              id: 'EAS_CRUD_SOURCE_MSG_LOAD_ERROR',
              defaultMessage: 'Chyba načtení dat: {detail}',
            });

          showSnackbar(message, SnackbarVariant.ERROR);

          throw err;
        }
      }
      return undefined;
    }
  });

  const handleCreate = useEventCallback(async (item: TYPE) => {
    try {
      setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = createItem(url, item);

      const data: TYPE = await fetch.current.json();

      const message =
        createMessages?.successMessage ??
        intl.formatMessage({
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
        if (handleCreateError !== undefined) {
          handleCreateError(err);
        } else {
          const message =
            createMessages?.errorMessage ??
            intl.formatMessage(
              {
                id: 'EAS_CRUD_SOURCE_MSG_CREATE_ERROR',
                defaultMessage: 'Chyba uložení dat: {detail}',
              },
              { detail: err.message }
            );

          showSnackbar(message, SnackbarVariant.ERROR);

          throw err;
        }
      }
      return undefined;
    }
  });

  const handleUpdate = useEventCallback(async (item: TYPE, prevItem: TYPE) => {
    try {
      setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = updateItem(url, item, prevItem);

      const data: TYPE = await fetch.current.json();

      const message =
        updateMessages?.successMessage ??
        intl.formatMessage({
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
        if (handleUpdateError !== undefined) {
          handleUpdateError(err);
        } else {
          const message =
            updateMessages?.errorMessage ??
            intl.formatMessage(
              {
                id: 'EAS_CRUD_SOURCE_MSG_UPDATE_ERROR',
                defaultMessage: 'Chyba uložení dat: {detail}',
              },
              { detail: err.message }
            );

          showSnackbar(message, SnackbarVariant.ERROR);

          throw err;
        }
      }
      return undefined;
    }
  });

  const handleDelete = useEventCallback(async (id: string) => {
    try {
      setLoading(true);
      if (fetch.current !== null) {
        fetch.current.abort();
      }

      fetch.current = deleteItem(url, id);

      await fetch.current.none();

      const message =
        delMessages?.successMessage ??
        intl.formatMessage({
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
        if (handleDeleteError !== undefined) {
          handleDeleteError(err);
        } else {
          const message =
            delMessages?.successMessage ??
            intl.formatMessage(
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
    }
  });

  const refresh = useEventCallback(async () => {
    if (data !== null) {
      return handleGet(data.id);
    }
  });

  const reset = useEventCallback((data?: TYPE) => {
    if (fetch.current !== null) {
      fetch.current.abort();
      fetch.current = null;
    }

    setData(data ?? null);
  });

  return {
    url,
    data,
    loading,
    setLoading,
    get: handleGet,
    create: handleCreate,
    update: handleUpdate,
    del: handleDelete,
    refresh,
    reset,
  };
}

/**
 * Fetches data for single item from API.
 *
 * @param api API url
 */
export function defaultGetItem(api: string, itemId: string) {
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
export function defaultCreateItem<TYPE>(api: string, item: TYPE) {
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
export function defaultUpdateItem<TYPE extends DomainObject>(
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
export function defaultDeleteItem(api: string, itemId: string) {
  return abortableFetch(`${api}/${itemId}`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'DELETE',
  });
}

export function getItemFactory<TYPE>({
  getItemMethod = defaultGetItem,
  postProcess = (data) => data,
}: {
  getItemMethod?: (api: string, itemId: string) => AbortableFetch;
  postProcess?: (data: TYPE) => TYPE | Promise<TYPE>;
}) {
  return function getItem(api: string, itemId: string) {
    const fetch = getItemMethod(api, itemId);

    const augmented: AbortableFetch = {
      response: fetch.response,
      abort: fetch.abort,
      json: async () => {
        const data = await fetch.json();
        const processedData = postProcess(data);

        if (processedData instanceof Promise) {
          return await processedData;
        } else {
          return processedData;
        }
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

export function updateItemFactory<TYPE extends DomainObject>({
  updateItemMethod = defaultUpdateItem,
  preProcess = (data) => data,
  postProcess = (data) => data,
}: {
  updateItemMethod?: (
    api: string,
    item: TYPE,
    initialItem: TYPE
  ) => AbortableFetch;
  preProcess?: (data: TYPE) => TYPE;
  postProcess?: (data: TYPE) => TYPE | Promise<TYPE>;
}) {
  return function updateItem(api: string, item: TYPE, initialItem: TYPE) {
    const preprocessed = preProcess(item);

    const fetch = updateItemMethod(api, preprocessed, initialItem);

    const augmented: AbortableFetch = {
      response: fetch.response,
      abort: fetch.abort,
      json: async () => {
        const data = await fetch.json();
        const processedData = postProcess(data);

        if (processedData instanceof Promise) {
          return await processedData;
        } else {
          return processedData;
        }
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

export function createItemFactory<TYPE extends DomainObject>({
  createItemMethod = defaultCreateItem,
  preProcess = (data) => data,
  postProcess = (data) => data,
}: {
  createItemMethod?: (api: string, item: TYPE) => AbortableFetch;
  preProcess?: (data: TYPE) => TYPE;
  postProcess?: (data: TYPE) => TYPE | Promise<TYPE>;
}) {
  return function createItem(api: string, item: TYPE) {
    const preprocessed = preProcess(item);

    const fetch = createItemMethod(api, preprocessed);

    const augmented: AbortableFetch = {
      response: fetch.response,
      abort: fetch.abort,
      json: async () => {
        const data = await fetch.json();
        const processedData = postProcess(data);

        if (processedData instanceof Promise) {
          return await processedData;
        } else {
          return processedData;
        }
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
