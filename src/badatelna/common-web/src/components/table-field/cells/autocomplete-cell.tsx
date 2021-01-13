import React, { useMemo } from 'react';
import { TableFieldCellProps } from '../table-field-types';
import { DomainObject } from 'common/common-types';
import { noop } from 'lodash';
import { AutocompleteSource } from 'components/autocomplete/autocomplete-types';
import { Autocomplete } from 'components/autocomplete/autocomplete';

export function useAutocompleteCellFactory<
  OBJECT,
  ATTRIBUTE extends DomainObject
>({
  dataHook,
  multiple = false,
}: {
  dataHook: () => AutocompleteSource<ATTRIBUTE>;
  multiple: boolean;
}) {
  return useMemo(
    () =>
      function AutocompleteCell({ value }: TableFieldCellProps<OBJECT>) {
        const source = dataHook();

        return (
          <Autocomplete
            disabled={true}
            source={source}
            value={value}
            onChange={noop}
            multiple={multiple}
          />
        );
      },
    [dataHook, multiple]
  );
}
