import { RefObject } from 'react';
import InfiniteLoader from 'react-window-infinite-loader';
import {
  Filter,
  Sort,
  ApiFilterOperation,
  ScrollableSource,
} from 'common/common-types';
import {
  TableSort,
  TableFilterWithState,
  TableFilter,
  TableFilterState,
  TableFilterOperation,
} from '../table-types';
import { useEventCallback } from 'utils/event-callback-hook';
import { useFirstRender } from 'utils/first-render';
import { useUpdateEffect } from 'utils/update-effect';
import { useForceRender } from 'utils/force-render';

export function useTableData<OBJECT>({
  source,
  searchQuery,
  sorts,
  filters,
  filtersState,
  loaderRef,
}: {
  source: ScrollableSource<OBJECT>;
  searchQuery: string;
  sorts: TableSort[];
  filters: TableFilter[];
  filtersState: TableFilterState[];
  loaderRef: RefObject<InfiniteLoader>;
}) {
  const { forceRender } = useForceRender();

  const setSourceParams = useEventCallback(() => {
    const filtersWithState: TableFilterWithState[] = filters.map(
      (filter, i) => ({ ...filter, ...filtersState[i] })
    );

    source.setParams({
      sort: convertToApiSorts(sorts),
      filters: convertToApiFilters([], filtersWithState, searchQuery),
      size: 30,
    });
  });

  useFirstRender(() => {
    setSourceParams();
  });

  useUpdateEffect(() => {
    setSourceParams();
    source.reset();
    forceRender();

    requestAnimationFrame(() => {
      loaderRef.current?.resetloadMoreItemsCache(true);
    });

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchQuery, sorts, filtersState]);
}

function convertToApiSorts(sorts: TableSort[]): Sort[] {
  return sorts.map((sort) => ({
    field: sort.field,
    order: sort.order,
    type: sort.type,
  }));
}

/**
 * Translares table filters to api filters.
 *
 * Combines pre-filters, table filters with search query, removes disabled or empty filters
 * @param preFilters
 * @param filters
 * @param searchQuery
 */
function convertToApiFilters(
  preFilters: Filter[],
  filters: TableFilterWithState[],
  searchQuery: string
) {
  const apiFilters = filters
    .filter((filter) => filter.enabled)
    .filter(filterEmptyFilter)
    .map((filter) => {
      const { operation, filterkey, value, filters } = filter;

      return {
        field: filterkey,
        operation: (operation as unknown) as ApiFilterOperation,
        value,
        filters,
      } as Filter;
    });

  if (searchQuery !== '') {
    apiFilters.push({
      field: '',
      operation: ApiFilterOperation.FTX,
      value: searchQuery,
    });
  }

  return [...preFilters, ...apiFilters];
}

function filterEmptyFilter({ value, operation }: TableFilter) {
  if (value == null && operation === TableFilterOperation.EQ) {
    return false;
  }

  return true;
}
