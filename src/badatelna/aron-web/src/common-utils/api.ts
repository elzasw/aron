import { useState, useEffect } from 'react';
import contentDisposition from 'content-disposition';
import { first, get } from 'lodash';

import { useFetch, ApiFilterOperation } from '@eas/common-web';

import { API_URL, ApiUrl, SortMode } from '../enums';
import { downloadFileFromUrl } from './file';
import { Filter, ApuPart, ApuPartItem } from '../types';
import { getUnitDatePart } from './date';

type Options = any;

export const createUrl = (url: string) => `${API_URL}${url}`;

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

export function useApiList<T>(url: string, options: Options = {}, callSource?: string) {
  return usePost<T>(`${url}/list?listType=${(callSource)?.toUpperCase()}`, {
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

export const useGetOptionsBySource = (
  source: string,
  query: string,
  isApuRef: boolean,
  apiFilters: Filter[]
) => {
  const field = isApuRef ? `${source}~ID~LABEL` : source;
  const fieldLabel = isApuRef ? `${source}~LABEL` : source;

  const [result, loading] = useApiList(ApiUrl.APU, {
    json: {
      size: 0,
      aggregations: [
        {
          family: 'BUCKET',
          aggregator: 'TERMS',
          name: 'items',
          field,
        },
      ],
      filters: [
        ...apiFilters.filter(
          ({ filters }) =>
            !filters || !filters.length || filters[0].field !== source
        ),
        ...(query
          ? [
              {
                field: fieldLabel,
                operation: ApiFilterOperation.CONTAINS,
                value: query,
              },
            ]
          : []),
      ],
    },
  }, "get-options-by-source");

  return [get(result, 'aggregations.items', []), loading];
};

export const useGetMatchingName = (query: string, group?: string) =>
  useApiList(ApiUrl.APU, {
    json: {
      filters: [
        group
          ? {
              operation: ApiFilterOperation.AND,
              filters: [
                {
                  operation: ApiFilterOperation.CONTAINS,
                  field: 'name',
                  value: query,
                },
                {
                  operation: ApiFilterOperation.EQ,
                  field: 'incomingRelTypeGroups',
                  value: group,
                },
              ],
            }
          : {
              operation: ApiFilterOperation.CONTAINS,
              field: 'name',
              value: query,
            },
      ],
      size: 10,
    },
  }, "get-matching-name");

export const useGetEntityRelationships = (
  id = '',
  apiFilters: Filter[],
  group?: string
) => {
  const [postResult, postLoading] = usePost(`${ApiUrl.APU}/list`+`?listType=GET-ENTITY-RELATIONSHIPS_${(group)?.toUpperCase()}`, {
    json: {
      size: 0,
      aggregations: [
        {
          family: 'BUCKET',
          aggregator: 'FILTER',
          name: 'apuFilterAgg',
          filter: {
            operation: ApiFilterOperation.OR,
            filters: apiFilters,
          },
          aggregations: [
            {
              name: 'nestedAgg',
              family: 'BUCKET',
              aggregator: 'NESTED',
              path: 'rels',
              aggregations: [
                {
                  family: 'BUCKET',
                  aggregator: 'FILTER',
                  name: 'relsFilterAgg',
                  filter: group
                    ? {
                        operation: ApiFilterOperation.AND,
                        filters: [
                          {
                            field: 'rels.targetId',
                            operation: ApiFilterOperation.EQ,
                            value: id,
                            nestedQueryEnabled: false,
                          },
                          {
                            operation: ApiFilterOperation.EQ,
                            field: 'rels.groups',
                            value: group,
                            nestedQueryEnabled: false,
                          },
                        ],
                      }
                    : {
                        field: 'rels.targetId',
                        operation: ApiFilterOperation.EQ,
                        value: id,
                        nestedQueryEnabled: false,
                      },
                  aggregations: [
                    {
                      family: 'BUCKET',
                      aggregator: 'TERMS',
                      name: 'relsTypeAgg',
                      field: 'rels.type',
                    },
                  ],
                },
              ],
            },
          ],
        },
      ],
    },
  });

  const fields =
    (postResult as any)?.aggregations?.apuFilterAgg[0]?.aggregations
      ?.nestedAgg[0]?.aggregations?.relsFilterAgg[0]?.aggregations
      ?.relsTypeAgg || [];
  return [fields, postLoading];
};

export const getFileByUrl = async (url: string) => {
  try {
    const response = await fetch(createUrl(url));

    if (response.ok) {
      const cd = response.headers.get('content-disposition');

      let name;

      if (cd) {
        const filename = contentDisposition.parse(cd).parameters.filename;

        name = filename && filename != 'null' ? filename : undefined;
      }

      return {
        blob: await response.blob(),
        name,
      };
    }
  } catch (error) {
    console.log(error);
  }

  return {};
};

export const getFile = async (id?: string, referencedFile?: boolean) => {
  if (id) {
    return await getFileByUrl(`${referencedFile ? ApiUrl.REFERENCED_FILES : ApiUrl.FILE}/${id}`);
  }

  return {};
};

export const downloadFile = async (
  id: string,
  filename?: string,
  blank?: boolean
) => {
  const { blob, name } = await getFile(id);

  if (blob) {
    downloadFileFromUrl(URL.createObjectURL(blob), name || filename, blank);
    return true;
  } else {
    return false;
  }
};

const useGetDateLimit = (
  sortMode: SortMode,
  filters: Filter[],
  field: string
): [number | null, boolean] => {
  const [response, loading] = useApiList(ApiUrl.APU, {
    json: {
      size: 1,
      sort: [
        {
          type: 'FIELD',
          field: field,
          sortMode,
          order: sortMode === SortMode.MAX ? 'DESC' : 'ASC',
        },
      ],
      // remove current field from filters
      filters: filters.filter(
        ({ field: filterField }) => field !== filterField
      ),
    },
  }, (`get-date-limit_${sortMode}`).toUpperCase());

  const items: any[] = get(response, 'items', []);

  const getYear = (years: number[]) =>
    years.length
      ? sortMode === SortMode.MAX
        ? Math.max(...years)
        : Math.min(...years)
      : null;

  const result =
    !loading && items.length
      ? getYear(
          first(items)
            ?.parts.reduce(
              (all: ApuPartItem[], current: ApuPart) => [
                ...all,
                ...current.items,
              ],
              []
            )
            .filter((item: ApuPartItem) => item.type === field)
            .map((item: ApuPartItem) =>
              getUnitDatePart(
                item.value,
                sortMode === SortMode.MAX ? 'to' : 'from'
              )
            )
            .map((item: string | null) =>
              item ? new Date(item).getFullYear() : null
            )
            .map((item: number) => item)
        )
      : null;

  return [result, loading];
};

export const useGetRangeFilterInterval = (
  filters: Filter[],
  field: string
): [[number | null, number | null], boolean] => {
  const [result, setResult] = useState<[number | null, number | null]>([
    null,
    null,
  ]);
  const [minValue, minLoading] = useGetDateLimit(SortMode.MIN, filters, field);
  const [maxValue, maxLoading] = useGetDateLimit(SortMode.MAX, filters, field);

  useEffect(() => {
    if (!minLoading && !maxLoading) {
      setResult([minValue || null, maxValue || null]);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [minLoading, maxLoading]);
  const loading = minLoading || maxLoading;
  return [result, loading];
};

export const getPageTemplateLogo = async () => {
  try {
    const response = await fetch(createUrl(ApiUrl.PAGE_TEMPLATE_LOGO), {
      headers: new Headers({
        'Content-Type': 'image/svg+xml',
      }),
    });

    return response.ok ? URL.createObjectURL(await response.blob()) : null;
  } catch (error) {
    console.log(error);
  }

  return null;
};

export const getPageTemplateTopImage = async () => {
  try {
    const response = await fetch(createUrl(ApiUrl.PAGE_TEMPLATE_TOP_IMAGE), {
      headers: new Headers({
        'Content-Type': 'image/png',
      }),
    });

    return response.ok ? URL.createObjectURL(await response.blob()) : null;
  } catch (error) {
    console.log(error);
  }

  return null;
};
