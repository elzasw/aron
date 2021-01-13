import { ApiFilterOperation, Params } from 'common/common-types';

export function eqFilterParams({
  field,
  value,
}: {
  field: string;
  value?: any;
}) {
  if (value === undefined) {
    return undefined;
  } else {
    return {
      filters: [
        {
          operation: ApiFilterOperation.EQ,
          field,
          value,
        },
      ],
    } as Params;
  }
}
