import { compact } from 'lodash';

import { ApiFilterOperation, Filter } from '../types';

export const toFilterOptions = (
  items: any[],
  createLabel: (item: any) => any,
  createValue: (item: any) => any
) =>
  items.map((item: any, index: number) => ({
    id: index.toString(),
    label: createLabel(item),
    value: createValue(item),
  }));

export const getTimeRangeLabel = (timeRange: { from: number; to: number }) =>
  `${timeRange.from ? timeRange.from : ''} - ${
    timeRange.to ? timeRange.to : ''
  }`;

export const filterFilters = (
  filters?: Filter[],
  additional = false
): Filter[] =>
  filters
    ? compact(
        filters.map(({ filters, ...item }) =>
          additional ||
          item.value ||
          ((item.operation === ApiFilterOperation.OR ||
            item.operation === ApiFilterOperation.AND) &&
            filters &&
            filters.length &&
            filtersNotEmpty(filters))
            ? {
                ...item,
                ...(filters && (!additional || filtersNotEmpty(filters))
                  ? { filters: filterFilters(filters) }
                  : {}),
              }
            : null
        )
      )
    : [];

export const filtersNotEmpty = (filters?: Filter[], additional = false) =>
  !!filterFilters(filters, additional).length;
