import { map, compact } from 'lodash';

import { ApiFilterOperation } from '@eas/common-web';

export const createUrlParams = (params: any) => {
  const query = map(params, (value, key) =>
    value ? `${encodeURIComponent(key)}=${encodeURIComponent(value)}` : ''
  )
    .filter((value) => value)
    .join('&');

  return `${query ? `?${query}` : ''}`;
};

const filterFilters = (filters?: any[]): any[] =>
  filters
    ? compact(
        filters.map(({ filters, ...item }) => {
          const isAndOrNot =
            item.operation === ApiFilterOperation.AND ||
            item.operation === ApiFilterOperation.OR ||
            item.operation === ApiFilterOperation.NOT;

          const filtersOk = filterFilters(filters).length;

          return item.value || (isAndOrNot && filtersOk)
            ? {
                ...item,
                ...(filtersOk ? { filters: filterFilters(filters) } : {}),
              }
            : null;
        })
      )
    : [];

export const createFiltersParam = (filters?: any[]) => {
  const filtered = filterFilters(filters);

  return filtered.length ? JSON.stringify(filtered) : '';
};

export const openInNewTab = (url: string) => {
  const a = document.createElement('a');
  a.href = /^https?:\/\/.*$/.test(url) ? url : `http://${url}`;
  a.target = '_blank';
  a.className = 'hidden';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
};

export const getURLWithChangedParam = (
  path: string,
  params: URLSearchParams,
  newParams: [string, any][]
) => {
  newParams.forEach(([name, value]) => params.set(name, value));
  return `${path}?${params}`;
};
