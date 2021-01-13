import React, { useState, useCallback, useEffect, useContext } from 'react';
import { useLocation } from 'react-router-dom';
import { get, find, isEqual, isEmpty, findIndex } from 'lodash';

import { NavigationContext } from '@eas/common-web';

import { EvidenceSidebar } from './evidence-sidebar';
import { EvidenceList } from './evidence-list';
import { MainProps, FiltersChangeCallbackParams } from './types';
import { ApiUrl, ModulePath, FilterType } from '../../enums';
import {
  ApiFilterOperation,
  Filter,
  FilterConfig,
  Relationship,
} from '../../types';
import {
  getTypeByPath,
  getURLWithChangedParam,
  filterFilters,
  useApiList,
} from '../../common-utils';
import { facetsToFilters, facetsContainRelationships } from './utils';
import { Module } from '../../components';

const getUrlParams = () => new URLSearchParams(window.location.search);

const getNumberParam = (key: string) => parseInt(getUrlParams().get(key) || '');

export function EvidenceMain({
  modulePath: path,
  label,
  facets,
  apuPartItemTypes,
}: MainProps) {
  const location = useLocation();

  const { navigate } = useContext(NavigationContext);

  const query = getUrlParams().get('query');

  const type = getTypeByPath(path);

  const [filtersConfig, setFiltersConfig] = useState<FilterConfig[]>([]);

  // if the relationship filter is not allowed, it's set to null
  const [relationships, setRelationships] = useState<Relationship[] | null>([]);

  const [page, setPage] = useState(1);

  const [pageSize, setPageSize] = useState(10);

  const updatePage = useCallback(
    (newValue) => {
      navigate(
        getURLWithChangedParam(path, getUrlParams(), [['page', newValue]])
      );
    },
    [path, navigate]
  );

  const updatePageSize = useCallback(
    (newValue) => {
      navigate(
        getURLWithChangedParam(path, getUrlParams(), [
          ['page', 1],
          ['pageSize', newValue],
        ])
      );
    },
    [path, navigate]
  );

  useEffect(() => {
    const loadFilters = async () => {
      const isApu = path === ModulePath.APU;

      const relationships = facetsContainRelationships(facets, path)
        ? JSON.parse(getUrlParams().get('relationships') || '[]')
        : null;
      isEmpty(relationships) && setRelationships(relationships);

      const filters = isApu
        ? []
        : (await facetsToFilters(facets, path, apuPartItemTypes)).filter(
            (f: FilterConfig) => f.type !== FilterType.RELATIONSHIP
          );
      const filtersFromURL = JSON.parse(
        getUrlParams().get('filters') || 'null'
      );
      if (filtersFromURL && !isEmpty(filtersFromURL)) {
        (filtersFromURL as FilterConfig[]).forEach((filterItem) => {
          const item = {
            ...filterItem,
            ...(filterItem.options
              ? {
                  options: filterItem.options.map((o, i) => ({
                    ...o,
                    id: `${i}`,
                  })),
                }
              : {}),
          };

          if (item.label) {
            if (item.value && !isEmpty(item.value)) {
              const patch: { filterObject?: any } = {};

              item.value.forEach((value) => {
                const option: any = find(
                  item.options,
                  (o) => o.value === value
                );

                patch.filterObject = {
                  ...patch.filterObject,
                  [`${option.id}`]: value,
                };
              });

              filters.push({
                ...item,
                ...patch,
              });
            }
          } else {
            const index = findIndex(
              filters,
              (f: any) => f.field === item.field
            );

            if (index > -1 && filters[index]) {
              if (item.value && !isEmpty(item.value)) {
                const patch: { filterObject?: any } = {};

                item.value.forEach((value) => {
                  const option = find(
                    filters[index].options,
                    (o) => o.value === value
                  );

                  patch.filterObject = {
                    ...patch.filterObject,
                    [`${option.id}`]: value,
                  };
                });

                filters[index] = {
                  ...filters[index],
                  ...patch,
                };
              }
            }
          }
        });
      }
      setFiltersConfig(filters);
    };
    loadFilters();
  }, [path, query, apuPartItemTypes, facets]);

  const [filters, setFilters] = useState([
    ...(type
      ? [{ field: 'type', operation: ApiFilterOperation.EQ, value: type }]
      : []),
    { operation: ApiFilterOperation.FTX, value: query },
  ] as Filter[]);

  const [result, loading] = useApiList(ApiUrl.APU, {
    json: {
      filters: filterFilters(filters),
      offset: (page - 1) * pageSize,
      size: pageSize,
    },
  });

  useEffect(() => {
    const parsedPage = getNumberParam('page');
    const parsedPageSize = getNumberParam('pageSize');

    const changePage = parsedPage && parsedPage !== page;

    const changePageSize = parsedPageSize && parsedPageSize !== pageSize;

    if (changePage) {
      setPage(parsedPage);
    }

    if (changePageSize) {
      setPageSize(parsedPageSize);
    }
  }, [location, page, pageSize]);

  const handleChange = useCallback(
    ({ query, filters: newFilters }: FiltersChangeCallbackParams) => {
      let newFiltersData = filters;
      const currentQuery = get(
        find(filters, (f) => f.operation === ApiFilterOperation.FTX),
        'value'
      );
      if (query !== currentQuery && (query || currentQuery)) {
        newFiltersData = isEmpty(
          filters.filter((f) => f.operation === ApiFilterOperation.FTX)
        )
          ? [...filters, { operation: ApiFilterOperation.FTX, value: query }]
          : filters.map((f) =>
              f.operation === ApiFilterOperation.FTX
                ? { ...f, value: query }
                : f
            );
      }

      newFiltersData = [
        ...newFiltersData.filter(
          ({ field, operation }) =>
            field === 'type' || operation === ApiFilterOperation.FTX
        ),
        ...(newFilters || []),
      ];
      if (!isEqual(filters, newFiltersData)) {
        updatePage(1);
        setFilters(newFiltersData);
      }
    },
    [filters, setFilters, updatePage]
  );

  return (
    <Module
      {...{
        items: [{ label }],
      }}
    >
      <>
        <EvidenceSidebar
          path={path}
          query={query || ''}
          filters={filtersConfig}
          onChange={handleChange}
          apuPartItemTypes={apuPartItemTypes}
          relationships={relationships}
          setRelationships={setRelationships}
        />
        <EvidenceList
          {...{
            loading,
            items: get(result, 'items', []),
            page,
            pageSize,
            updatePage,
            updatePageSize,
            count: Math.ceil(get(result, 'count', 1) / pageSize),
          }}
        />
      </>
    </Module>
  );
}
