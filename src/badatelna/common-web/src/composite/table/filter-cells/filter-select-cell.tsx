import React, { useMemo } from 'react';
import { Select } from 'components/select/select';
import { FilterComponentProps } from '../table-types';
import { ListSource } from 'common/common-types';

export function useFilterSelectCellFactory<OBJECT>(
  dataHook: () => ListSource<OBJECT>
) {
  return useMemo(
    () =>
      function FilterSelectCell({
        disabled,
        value,
        onChange,
      }: FilterComponentProps) {
        const source = dataHook();

        return (
          <Select
            disabled={disabled}
            source={source}
            value={value}
            onChange={onChange}
            valueIsId={true}
          />
        );
      },
    [dataHook]
  );
}
