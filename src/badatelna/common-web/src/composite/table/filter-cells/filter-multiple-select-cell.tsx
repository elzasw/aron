import React, { useMemo } from 'react';
import { Select } from 'components/select/select';
import { FilterComponentProps, TableFilterOperation } from '../table-types';
import {
  ListSource,
  DomainObject,
  ApiFilterOperation,
  Filter,
} from 'common/common-types';
import { useEventCallback } from 'utils/event-callback-hook';

export function useFilterMultipleSelectCellFactory<OBJECT extends DomainObject>(
  dataHook: () => ListSource<OBJECT>
) {
  return useMemo(
    () =>
      function FilterSelectCell({
        disabled,
        filter,
        state,
        onChangeState,
      }: FilterComponentProps) {
        const source = dataHook();

        const handleChange = useEventCallback((value: string[]) => {
          onChangeState({
            ...state,
            operation: TableFilterOperation.OR,
            filters: constructNestedFilters(filter.filterkey, value),
            selectFilter: value,
          });
        });

        return (
          <Select
            disabled={disabled}
            source={source}
            value={state.selectFilter ?? []}
            onChange={handleChange as any}
            selectableAll={false}
            clearable={false}
            valueIsId
            multiple
          />
        );
      },
    [dataHook]
  );
}

function constructNestedFilters(field: string, values: string[]) {
  return values.map((value) => ({
    field,
    operation: ApiFilterOperation.EQ,
    value,
  })) as Filter[];
}
