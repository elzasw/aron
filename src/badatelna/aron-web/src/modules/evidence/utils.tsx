import React from 'react';

import { FilterType, ModulePath } from '../../enums';
import { SelectionFilter } from './sidebar-content/selection-filter';
import { FilterChangeCallBack } from './types';
import InputFilter from './sidebar-content/input-filter';
import {
  getSearchUrlWithFilters,
  getTypeByPath,
  getEnumsOptions,
} from '../../common-utils';
import {
  ApiFilterOperation,
  Facet,
  FilterConfig,
  FacetType,
  ApuPartItemType,
  Relationship,
  Filter,
  AggregationItems,
  AggregationItem,
} from '../../types';
import { find, isEmpty, get } from 'lodash';
import { toFilterOptions, getTimeRangeLabel } from '../../common-utils';
import RelationshipFilter from './sidebar-content/relationship-filter';

export const findApuParts = (items: any[], code: string) =>
  items.filter(({ type }) => type === code);

export const filterApuPartTypes = (items: any[], entityItems: any[]) =>
  items.filter(({ code }) => findApuParts(entityItems, code).length);

export const getFilterComponent = ({
  type,
  index = 0,
  onChange = () => null,
  ...props
}: {
  type: FilterType;
  index: number;
  onChange: FilterChangeCallBack;
}) => {
  let FilterComponent: React.ReactType;
  switch (type) {
    case FilterType.SELECT:
    case FilterType.RADIO:
    case FilterType.CHECKBOX_WITH_RANGE:
    case FilterType.CHECKBOX:
      FilterComponent = SelectionFilter;
      break;
    case FilterType.INPUT:
    case FilterType.INPUT_MULTI:
      FilterComponent = InputFilter;
      break;
    case FilterType.RELATIONSHIP:
      FilterComponent = RelationshipFilter;
      break;
    default:
      FilterComponent = () => null;
      break;
  }
  return (
    <FilterComponent
      {...{
        ...props,
        type,
        onChange,
        multiple: type === FilterType.INPUT_MULTI ? true : false,
        autocomplete: type === FilterType.INPUT_MULTI ? true : false,
      }}
      key={index}
    />
  );
};

export const getRelatedApusURL = (name: string, id: string): string =>
  getSearchUrlWithFilters([
    {
      type: FilterType.RADIO,
      label: `Související s: ${name}`,
      options: [{ label: 'Ano', value: id }],
      value: [id],
      operation: ApiFilterOperation.AND,
      filters: [
        {
          operation: ApiFilterOperation.NOT,
          filters: [
            {
              field: 'id',
              operation: 'EQ',
              value: id,
            },
          ],
        },
        {
          operation: ApiFilterOperation.AKF,
          value: id,
        },
      ],
    },
  ]);

export const retypeFacetToFilter = (type: FacetType): FilterType => {
  switch (type) {
    case FacetType.ENUM:
      return FilterType.CHECKBOX;
    case FacetType.FULLTEXT:
      return FilterType.INPUT;
    case FacetType.UNITDATE:
      return FilterType.CHECKBOX_WITH_RANGE;
    case FacetType.MULTI_REF:
      return FilterType.INPUT_MULTI;
    case FacetType.MULTI_REF_EXT:
      return FilterType.RELATIONSHIP;
  }
};

const parseToISO = (date: string) => new Date(date).toISOString();

export const parseFacetOptions = (
  facet: Facet,
  enumsOptions: AggregationItems
) => {
  switch (facet.type) {
    case FacetType.UNITDATE:
      return toFilterOptions(
        facet.intervals,
        (i) => getTimeRangeLabel({ from: i.fromText, to: i.toText }),
        ({ from, to }) => ({ from: parseToISO(from), to: parseToISO(to) })
      );
    case FacetType.ENUM:
      return toFilterOptions(
        get(enumsOptions, facet.source, []),
        (o: AggregationItem) => `${o.key} ( ${o.value} )`, // TODO: `${o[`${facet.source}~LABEL`]} ( ${o.value} )`,
        (o: AggregationItem) => o.key
      );
    default:
      return [];
  }
};

const isFilterWithOptions = (type: FilterType) => {
  switch (type) {
    case FilterType.SELECT:
    case FilterType.CHECKBOX:
    case FilterType.CHECKBOX_WITH_RANGE:
    case FilterType.RADIO:
      return true;
    default:
      return false;
  }
};
export const facetsToFilters = async (
  facets: Facet[],
  path: ModulePath,
  apuPartItemTypes: ApuPartItemType[]
): Promise<FilterConfig[]> => {
  const actualFacets = facets.filter(
    (facet: Facet) => facet.when.apuType === getTypeByPath(path)
  );
  const enumsOptions = (
    await getEnumsOptions(
      actualFacets
        .filter((facet: Facet) => facet.type === FacetType.ENUM)
        .map((facet: Facet) => facet.source)
    )
  ).aggregations;

  return actualFacets
    .map((facet: Facet) => ({
      type: retypeFacetToFilter(facet.type),
      label: find(apuPartItemTypes, { code: facet.source })?.name || '_',
      field: facet.source,
      value: [],
      options: parseFacetOptions(facet, enumsOptions),
    })) //filters out ENUM filters with no options
    .filter(
      ({ type, options }) => !isFilterWithOptions(type) || !isEmpty(options)
    );
};

export const facetsContainRelationships = (
  facets: Facet[],
  path: ModulePath
): boolean =>
  !isEmpty(
    find(
      facets.filter((f: Facet) => f.when.apuType === getTypeByPath(path)),
      { type: FacetType.MULTI_REF_EXT }
    )
  );

export const convertRelationshipsToFilter = (
  relationships: Relationship[]
): Filter | undefined =>
  relationships && {
    operation: ApiFilterOperation.OR,
    filters: relationships.map(({ field, value }: Relationship) => ({
      operation: ApiFilterOperation.EQ,
      field,
      value,
    })),
  };
