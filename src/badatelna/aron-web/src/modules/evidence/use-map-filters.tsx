import React from 'react';
import { find, isEmpty, get, compact, isArray, map, orderBy } from 'lodash';
import { useIntl } from 'react-intl';

import {
  FacetType,
  FacetDisplay,
  Message,
  ApuPartItemDataType,
  ApiUrl,
} from '../../enums';
import {
  getApuPartItemType,
  parseApuRefOptionId,
  parseApuRefOptionLabel,
  createUrl,
} from '../../common-utils';
import {
  Facet,
  FilterConfig,
  ApuPartItemType,
  AggregationItems,
  BasicFilterConfig,
  Filter,
} from '../../types';
import { filterApiFilters, createApiFilters } from './utils';
import { TextWithCount } from '../../components';

const isFilterWithOptions = (type: FacetType) => {
  switch (type) {
    case FacetType.ENUM:
    case FacetType.ENUM_SINGLE:
      return true;
    default:
      return false;
  }
};

const getEnumOptions = async (
  source: string,
  apuPartItemTypes: ApuPartItemType[],
  filters: FilterConfig[],
  additionalFilters: Filter[],
) => {
  const response = await fetch(createUrl(`${ApiUrl.APU}/list`), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      size: 0,
      aggregations: [
        {
          family: 'BUCKET',
          aggregator: 'TERMS',
          name: source,
          field:
            getApuPartItemType(apuPartItemTypes, source) ===
            ApuPartItemDataType.APU_REF
              ? `${source}~ID~LABEL`
              : source,
          size: 9999,
        },
      ],
      filters: filterApiFilters([
        ...additionalFilters,
        ...createApiFilters(filters.filter((f) => f.source !== source)),
      ]),
    }),
  });

  return await response.json();
};

export const getEnumsOptions = async (
  sources: string[],
  apuPartItemTypes: ApuPartItemType[],
  filters: FilterConfig[],
  additionalFilters: Filter[],
) => {
  try {
    const promisses = sources.map((source) =>
      getEnumOptions(source, apuPartItemTypes, filters, additionalFilters)
    );

    let result = {};

    compact(await Promise.all(promisses)).forEach(
      (item) => (result = { ...result, ...item.aggregations })
    );

    return result;
  } catch (e) {
    console.log(e);
    return {};
  }
};

export function useMapFilters() {
  const { formatMessage } = useIntl();

  const parseEnumOptions = (
    item: FilterConfig,
    enumsOptions: AggregationItems,
    apuPartItemTypes: ApuPartItemType[]
  ) => {
    const isApuRef =
      getApuPartItemType(apuPartItemTypes, item.source) ===
      ApuPartItemDataType.APU_REF;

    switch (item.type) {
      case FacetType.ENUM:
        return orderBy(
          map(get(enumsOptions, item.source, []), (optionItem: any) => {
            const labelString = isApuRef
              ? parseApuRefOptionLabel(optionItem.key)
              : optionItem.key;

            return {
              label: (
                <TextWithCount text={labelString} count={optionItem.value} />
              ),
              labelString,
              value: isApuRef
                ? parseApuRefOptionId(optionItem.key)
                : optionItem.key,
              count: parseInt(optionItem.value, 10),
            };
          }),
          item.orderBy === 'ASC' ? ['labelString'] : ['count'],
          item.orderBy === 'ASC' ? ['asc'] : ['desc']
        ).map(({ label, labelString, value }) => ({
          label,
          labelString,
          value,
        }));
      default:
        return [];
    }
  };

  const mapFilters = async (
    facets: Facet[],
    apuPartItemTypes: ApuPartItemType[],
    filters: FilterConfig[],
    additionalFilters: Filter[],
  ): Promise<FilterConfig[]> => {
    const merged: BasicFilterConfig[] = [...facets];

    // add filter definition from URL filters
    filters.forEach((item) => {
      const isSpecialFilter =
        item.type === FacetType.DAO_ONLY ||
        item.type === FacetType.RELATED_APUS;

      if (
        item.type &&
        (isSpecialFilter ||
          (item.source &&
            !find(facets, ({ source }) => source === item.source)))
      ) {
        const newItem = {
          source: FacetType.RELATED_APUS,
          ...(item as any),
        };

        // special filter is first
        if (isSpecialFilter) {
          merged.unshift(newItem);
        } else {
          merged.push(newItem);
        }
      }
    });

    const mapped = merged.map((item) => {
      return {
        display: FacetDisplay.ALWAYS,
        displayedItems: 0,
        maxDisplayedItems: 0,
        orderBy: 'FREQ',
        ...item,
        label:
          item.type === FacetType.DAO_ONLY
            ? formatMessage({ id: Message.DAO_ONLY })
            : item.type === FacetType.RELATED_APUS
            ? `${formatMessage({ id: Message.RELATED_TO })}: ${item.value}`
            : find(apuPartItemTypes, { code: item.source })?.name || '_',
        value:
          item.value ||
          find(filters, ({ source }) => source === item.source)?.value ||
          (item.type === FacetType.FULLTEXT ? '' : []),
      };
    });

    const enumsOptions = await getEnumsOptions(
      merged
        .filter((item) => item.type === FacetType.ENUM)
        .map((item) => item.source),
      apuPartItemTypes,
      mapped,
      additionalFilters
    );

    // add options
    return mapped
      .map((item) => {
        const options =
          item.options ||
          parseEnumOptions(item, enumsOptions, apuPartItemTypes);

        return {
          ...item,
          options: (isArray(item.order) && item.order.length
            ? [
                ...compact(
                  item.order.map((orderItem) =>
                    find(
                      options,
                      (o) =>
                        orderItem === o.value || orderItem === o.labelString
                    )
                  )
                ),
                ...options.filter(
                  (o) =>
                    !find(
                      item.order,
                      (orderItem) =>
                        orderItem === o.value || orderItem === o.labelString
                    )
                ),
              ]
            : options
          ).map((option) => {
            const tooltip = find(
              (item as any).tooltips,
              ({ value }) =>
                value === option.value || value === option.labelString
            )?.tooltip;

            return { ...option, ...(tooltip ? { tooltip } : {}) };
          }),
        };
      })
      .filter(
        ({ type, options }) => !isFilterWithOptions(type) || !isEmpty(options)
      );
  };

  return mapFilters;
}
