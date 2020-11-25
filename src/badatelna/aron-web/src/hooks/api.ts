import { useFetch } from '@eas/common-web';

type Options = any;

const BASE_URL = '/api/aron';

const useApi = (url: string, options: Options = {}) =>
  useFetch(`${BASE_URL}${url}`, {
    ...options,
    ...(options.json
      ? {
          body: JSON.stringify(options.json),
        }
      : {}),
  });

export const useGet = (url: string, options: Options = {}) =>
  useApi(url, options);

export const usePost = (url: string, options: Options = {}) =>
  useApi(url, {
    method: 'POST',
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    ...options,
  });

export const useApiList = (url: string, options: Options = {}) =>
  usePost(`${url}/list`, {
    ...options,
    json: { size: -1, flipDirection: false, ...(options.json || {}) },
  });
