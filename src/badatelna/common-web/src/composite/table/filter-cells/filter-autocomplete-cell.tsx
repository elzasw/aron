import React, { useMemo } from 'react';
import { FilterComponentProps, TableFilterOperation } from '../table-types';
import { AutocompleteSource } from 'components/autocomplete/autocomplete-types';
import { Autocomplete } from 'components/autocomplete/autocomplete';
import { useEventCallback } from 'utils/event-callback-hook';
import { DomainObject, ApiFilterOperation } from 'common/common-types';
import { isArray } from 'lodash';

export function useFilterAutocompleteCellFactory<OBJECT extends DomainObject>(
  dataHook: () => AutocompleteSource<OBJECT>,
  labelMapper?: (option: OBJECT) => string
) {
  return useMemo(
    () =>
      function FilterAutocompleteCell({
        disabled,
        state,
        filter,
        onChangeState,
      }: FilterComponentProps) {
        const source = dataHook();

        const handleChange = useEventCallback(
          (value: OBJECT | OBJECT[] | null) => {
            if (isArray(value)) {
              onChangeState({
                ...state,
                operation: TableFilterOperation.AND,
                filters: constructNestedFilters(filter.filterkey, value),
                object: value,
              });
            } else {
              onChangeState({
                ...state,
                operation: TableFilterOperation.EQ,
                value: value?.id,
                object: value,
              });
            }
          }
        );

        return (
          <Autocomplete
            disabled={disabled}
            source={source}
            labelMapper={labelMapper}
            value={state.object}
            onChange={handleChange}
          />
        );
      },
    [dataHook]
  );
}

function constructNestedFilters<OBJECT extends DomainObject>(
  field: string,
  array?: OBJECT[]
) {
  return (array ?? []).map((o) => ({
    field: field,
    operation: ApiFilterOperation.EQ,
    value: o.id,
  }));
}
