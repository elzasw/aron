import { isEmpty, compact } from 'lodash';

import { useApiList } from '../../../common-utils';
import { ApiFilterOperation, Filter } from '../../../types';
import { ApiUrl, DEFAULT_RANGE } from '../../../enums';

export const toStrRange = (numberRange: [number, number]): [string, string] => [
  numberRange[0].toString(),
  numberRange[1].toString(),
];

export const isOutsideRange = (value: string) => {
  const parsed = parseInt(value);

  return (
    !parsed ||
    isEmpty(value) ||
    DEFAULT_RANGE[0] > parsed ||
    DEFAULT_RANGE[1] < parsed
  );
};

export const yearInISO = (value: number | null, isGTE = false) =>
  value
    ? `${`0${value}`.slice(-4)}-${
        isGTE ? `01-01T00:00:00.000Z` : `12-31T23:59:59.999Z`
      }`
    : null;

export const useGetCountRange = (
  field: string,
  value: [number, number],
  apiFilters: Filter[]
) =>
  useApiList<{ count: number }>(ApiUrl.APU, {
    json: {
      size: 0,
      filters: compact([
        // remove current field from filters
        ...apiFilters.filter((item) => item.field !== field),
        {
          field,
          operation: ApiFilterOperation.RANGE,
          gte: yearInISO(Math.max(value[0], DEFAULT_RANGE[0]), true),
          lte: yearInISO(Math.min(value[1], DEFAULT_RANGE[1])),
        },
      ]),
    },
  });

export const useGetCountInput = (
  field: string,
  value: string,
  apiFilters: Filter[]
) =>
  useApiList<{ count: number }>(ApiUrl.APU, {
    json: {
      size: 0,
      filters: compact([
        // remove current field from filters
        ...apiFilters.filter((item) => item.field !== field),
        ...(value
          ? [
              {
                field,
                operation: ApiFilterOperation.CONTAINS,
                value,
              },
            ]
          : []),
      ]),
    },
  });
