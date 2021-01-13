import { useFetch, ApiFilterOperation } from '@eas/common-web';

import { API_URL, ApiUrl } from '../enums';
import { downloadFileFromUrl } from './file';

type Options = any;

const createUrl = (url: string) => `${API_URL}${url}`;

function useApi<T>(url: string, options: Options = {}) {
  return useFetch<T>(createUrl(url), {
    ...options,
    ...(options.json
      ? {
          body: JSON.stringify(options.json),
        }
      : {}),
  });
}

export function useGet<T>(url: string, options: Options = {}) {
  return useApi<T>(url, options);
}

export function usePost<T>(url: string, options: Options = {}) {
  return useApi<T>(url, {
    method: 'POST',
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    ...options,
  });
}

export function useApiList<T>(url: string, options: Options = {}) {
  return usePost<T>(`${url}/list`, {
    ...options,
    json: { size: -1, flipDirection: false, ...(options.json || {}) },
  });
}

export const getApu = async (id: string) => {
  try {
    const response = await fetch(createUrl(`${ApiUrl.APU}/${id}`));

    return response.ok ? await response.json() : null;
  } catch (error) {
    console.log(error);
    return null;
  }
};

// TODO: after conclusion, edit the way of getting the autocomplete options
export const useGetOptionsByField = (
  query: string,
  field: string
): [any[], boolean] => {
  return [[], false];
  // return useApiList(ApiUrl.APU, {
  //   json: {
  //     size: 0,
  //     aggregations: [
  //       {
  //         family: 'BUCKET',
  //         aggregator: 'TERMS',
  //         name: 'items',
  //         field: `${field}~LABEL`,
  //       },
  //     ],
  //   },
  // });
};

export const useGetMatchingName = (query: string) =>
  useApiList(ApiUrl.APU, {
    json: {
      filters: [
        {
          operation: ApiFilterOperation.CONTAINS,
          field: 'name',
          value: query,
        },
      ],
      size: 10,
    },
  });

export const useGetEntityRelationships = (id = '') => {
  const [postResult, postLoading] = usePost(`${ApiUrl.RELATION}/list`, {
    json: {
      size: 0,
      aggregations: [
        {
          family: 'BUCKET',
          aggregator: 'FILTER',
          name: 'outer',
          filter: {
            field: 'target',
            operation: ApiFilterOperation.EQ,
            value: id,
          },
          aggregations: [
            {
              family: 'BUCKET',
              aggregator: 'TERMS',
              name: 'inner',
              field: 'relation',
            },
          ],
        },
      ],
    },
  });
  const fields =
    (postResult as any)?.aggregations?.outer[0]?.aggregations?.inner || [];
  return [fields, postLoading];
};

export const getFile = async (id?: string) => {
  if (!id) return null;
  try {
    const response = await fetch(createUrl(`${ApiUrl.FILE}/${id}`));

    return response.ok ? await response.blob() : null;
  } catch (error) {
    console.log(error);
    return null;
  }
};

export const downloadApiFile = async (id: string, name: string) => {
  const fileBlob = await getFile(id);

  if (fileBlob) {
    downloadFileFromUrl(URL.createObjectURL(fileBlob), name);
  }
};

export const getEnumsOptions = async (fields: string[]) => {
  try {
    const response = await fetch(createUrl(`${ApiUrl.APU}/list`), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        size: 0,
        aggregations: fields.map((field: string) => ({
          family: 'BUCKET',
          aggregator: 'TERMS',
          name: field,
          field,
        })),
      }),
    });
    const result = await response.json();
    return result;
  } catch (e) {
    console.log(e);
    return [];
  }
};
