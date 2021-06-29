import React, {
  createContext,
  useCallback,
  useMemo,
  useContext,
  ReactNode,
  useEffect,
  useState,
} from 'react';
import { useHistory } from 'react-router-dom';
import { find, pick, isArray, compact, uniqBy } from 'lodash';

import { useEventCallback } from '@eas/common-web';

import {
  getTypeByPath,
  useEvidenceNavigation,
  getApu,
} from '../../common-utils';
import { ModulePath, FacetType } from '../../enums';
import {
  Facet,
  ApiFilterOperation,
  FilterConfig,
  Relationship,
} from '../../types';
import {
  createApiFilters,
  filterApiFilters,
  filterFacets,
  filterMappedFilters,
} from './utils';
import { useMapFilters } from './use-map-filters';

interface Filter {
  source?: string;
  value?: string | null;
  operation: ApiFilterOperation;
}

interface RelationshipName {
  name: string;
  value: string;
}

interface IFilterContext {
  loading: boolean;
  initialized: boolean;
  page: number;
  pageSize: number;
  query: string;
  filters: FilterConfig[];
  apiFilters: Filter[];
  apiFiltersOnly: Filter[];
  typeFilter: Filter;
  queryFilter: Filter;
  updatePage: (page: number) => void;
  updatePageSize: (pageSize: number) => void;
  updateQuery: (query: string) => void;
  updateFilter: (newFilter: FilterConfig) => void;
  updateFilters: (newFilter: FilterConfig[]) => void;
}

const FiltersContext = createContext<IFilterContext>(undefined as any);

interface FilterContextProps {
  path: ModulePath;
  facets: Facet[];
  apuPartItemTypes: any;
}

function useFiltersContext({
  path,
  facets: allFacets,
  apuPartItemTypes,
}: FilterContextProps) {
  /*********************************
   *
   * Prepare data from params
   *
   *********************************/

  const facets = filterFacets(allFacets, path);

  /*********************************
   *
   * Hooks
   *
   *********************************/

  const history = useHistory();

  const navigateTo = useEvidenceNavigation();

  const mapFilters = useMapFilters();

  /*********************************
   *
   * State
   *
   *********************************/

  const [loading, setLoading] = useState(false);
  const [initialized, setInitialized] = useState(false);

  const [relationshipNames, setRelationshipNames] = useState<
    RelationshipName[]
  >([] as RelationshipName[]);

  const [{ page, pageSize, query, filtersString }, updateState] = useState({
    page: 1,
    pageSize: 10,
    query: '',
    filtersString: '',
  });

  const [filters, setFilters] = useState<FilterConfig[]>([]);

  const typeFilter = useMemo(
    () => ({
      field: 'type',
      operation: ApiFilterOperation.EQ,
      value: getTypeByPath(path),
    }),
    [path]
  );

  const queryFilter = useMemo(
    () => ({ operation: ApiFilterOperation.FTX, value: query }),
    [query]
  );

  const apiFiltersOnly = useMemo(
    () => filterApiFilters(createApiFilters(filters)),
    [filters]
  );

  const apiFilters = useMemo(
    () => filterApiFilters([typeFilter, queryFilter, ...apiFiltersOnly]),
    [typeFilter, queryFilter, apiFiltersOnly]
  );

  /*********************************
   *
   * Helpers
   *
   *********************************/

  const updateRelationshipNames = useCallback(
    (newRelationshipsNames: RelationshipName[]) => {
      if (newRelationshipsNames.length) {
        const merged = uniqBy(
          [...newRelationshipsNames, ...relationshipNames],
          'value'
        );

        setRelationshipNames(merged);

        return merged;
      }

      return relationshipNames;
    },
    [relationshipNames]
  );

  const changeUrl = useCallback(
    (
      page: number,
      pageSize: number,
      query: string,
      filters: FilterConfig[]
    ) => {
      let relationshipNames: RelationshipName[] = [];

      filters = filters.map((f) => {
        if (f.type === FacetType.MULTI_REF_EXT) {
          const r = f.value as Relationship[];

          relationshipNames = [
            ...relationshipNames,
            ...(r
              .filter(({ name }) => name)
              .map((v) => pick(v, ['name', 'value'])) as RelationshipName[]),
          ];

          return {
            ...f,
            value: r.map((v) => pick(v, ['field', 'value'])),
          };
        }

        return f;
      });

      updateRelationshipNames(relationshipNames);

      navigateTo(
        path,
        page,
        pageSize,
        query,
        filters
          .map((f) =>
            find(facets, (facet) => facet.source === f.source)
              ? pick(f, ['source', 'value'])
              : f
          )
          .filter(({ value }) => value && (!isArray(value) || value.length))
      );
    },
    [path, navigateTo, facets, updateRelationshipNames]
  );

  const updateFromUrl = useCallback(
    (location) => {
      const params = new URLSearchParams(location.search);

      const getParam = (
        param: string,
        defaultValue: any,
        isNumber?: boolean
      ) => {
        let parsed: any = params.get(param);

        parsed = isNumber
          ? !isNaN(Number(parsed))
            ? Number(parsed)
            : null
          : parsed;

        return parsed || defaultValue;
      };

      const newPage = getParam('p', 1, true);
      const newPageSize = getParam('s', 10, true);
      const newQuery = getParam('q', '');
      const newFiltersString = getParam('f', '');

      if (!initialized && !newFiltersString) {
        setInitialized(true);
      }

      if (
        newPage !== page ||
        newPageSize !== pageSize ||
        newQuery !== query ||
        newFiltersString !== filtersString
      ) {
        updateState({
          page: newPage,
          pageSize: newPageSize,
          query: newQuery,
          filtersString: newFiltersString,
        });
      }
    },
    [page, pageSize, query, filtersString, initialized]
  );

  const parseFacets = useCallback(async () => {
    setLoading(true);

    if (!initialized) {
      setInitialized(true);
    }

    let newFiltersFromUrl = [];

    try {
      newFiltersFromUrl = JSON.parse(filtersString);
    } catch (error) {
      newFiltersFromUrl = [];
    }

    const mappedFilters = await mapFilters(
      facets,
      apuPartItemTypes,
      newFiltersFromUrl,
      typeFilter,
      queryFilter
    );

    const newFilters = filterMappedFilters(mappedFilters, path);

    const promisses: Promise<any>[] = [];

    newFilters.forEach((f) => {
      if (f.type === FacetType.MULTI_REF_EXT) {
        (f.value as Relationship[]).forEach((v) => {
          if (!find(relationshipNames, ({ value }) => value === v.value)) {
            promisses.push(getApu(v.value));
          }
        });
      }
    });

    let results = [];

    try {
      results = compact(await Promise.all(promisses));
    } catch (error) {
      console.log(error);
      results = [];
    }

    const newRelationshipsNames = updateRelationshipNames(
      results.map(({ id, name }) => ({ value: id, name }))
    );

    setFilters(
      newFilters.map((f) =>
        f.type === FacetType.MULTI_REF_EXT
          ? {
              ...f,
              value: (f.value as Relationship[]).map((v) => ({
                ...v,
                name:
                  find(newRelationshipsNames, ({ value }) => value === v.value)
                    ?.name || v.value,
              })),
            }
          : f
      )
    );

    setLoading(false);
  }, [
    path,
    facets,
    filtersString,
    apuPartItemTypes,
    relationshipNames,
    updateRelationshipNames,
    mapFilters,
    typeFilter,
    queryFilter,
    initialized,
  ]);

  /*********************************
   *
   * Effects
   *
   *********************************/

  useEffect(() => {
    updateFromUrl(window.location);

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    setTimeout(parseFacets);

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtersString, query]);

  useEffect(() => {
    return history.listen(updateFromUrl);
  }, [history, updateFromUrl]);

  /*********************************
   *
   * Handlers
   *
   *********************************/

  const updatePage = useCallback(
    (newValue: number) => {
      changeUrl(newValue, pageSize, query, filters);
    },
    [changeUrl, pageSize, query, filters]
  );

  const updatePageSize = useCallback(
    (newValue: number) => {
      changeUrl(1, newValue, query, filters);
    },
    [changeUrl, query, filters]
  );

  const updateQuery = useCallback(
    (newValue: string) => {
      changeUrl(1, pageSize, newValue, filters);
    },
    [changeUrl, pageSize, filters]
  );

  // useEventCallback is needed
  const updateFilter = useEventCallback((newFilter: FilterConfig) => {
    changeUrl(
      1,
      pageSize,
      query,
      filterMappedFilters(
        filters.map((f) =>
          f.source === newFilter.source ? { ...f, value: newFilter.value } : f
        ),
        path
      )
    );
  });

  const updateFilters = useEventCallback((newFilters: FilterConfig[]) => {
    changeUrl(1, pageSize, query, filterMappedFilters(newFilters, path));
  });

  /*********************************
   *
   * Context
   *
   *********************************/

  const context: IFilterContext = useMemo(
    () => ({
      loading,
      initialized,
      page,
      pageSize,
      query,
      filters,
      apiFilters,
      apiFiltersOnly,
      typeFilter,
      queryFilter,
      updatePage,
      updatePageSize,
      updateQuery,
      updateFilter,
      updateFilters,
    }),
    [
      loading,
      initialized,
      page,
      pageSize,
      query,
      filters,
      apiFilters,
      apiFiltersOnly,
      typeFilter,
      queryFilter,
      updatePage,
      updatePageSize,
      updateQuery,
      updateFilter,
      updateFilters,
    ]
  );

  return { context };
}

export function FiltersProvider({
  children,
  ...props
}: { children: ReactNode } & FilterContextProps) {
  const { context } = useFiltersContext(props);

  return (
    <FiltersContext.Provider value={context}>
      {children}
    </FiltersContext.Provider>
  );
}

export function useFilters() {
  return useContext(FiltersContext);
}
