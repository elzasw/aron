import { ModulePath } from '../enums';

import { filterFilters, filtersNotEmpty } from './filter';

const createUrlParam = (param: any, text: string) =>
  param ? `${text}=${encodeURI(param)}` : '';

const createFiltersParam = (filters?: any[]) =>
  filtersNotEmpty(filters, true)
    ? createUrlParam(JSON.stringify(filterFilters(filters, true)), 'filters')
    : '';

export const getUrlWithQuery = (path: string, query = '', filters?: any[]) => {
  const includeFilters = filtersNotEmpty(filters, true);
  console.log(
    'filterFilters(filters, true) :>> ',
    filterFilters(filters, true)
  );
  return `${path}${query || includeFilters ? '?' : ''}${createUrlParam(
    query,
    'query'
  )}${includeFilters ? '&' : ''}${createFiltersParam(filters)}`;
};

export const getSearchUrlWithFilters = (filters: any[]) =>
  `${ModulePath.APU}?${createFiltersParam(filters)}`;

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
