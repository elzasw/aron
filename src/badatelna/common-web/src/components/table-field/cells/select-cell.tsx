import React, { useMemo, ComponentType } from 'react';
import { TableFieldCellProps } from '../table-field-types';
import { ListSource, DomainObject } from 'common/common-types';
import { Select } from 'components/select/select';
import { noop } from 'lodash';

export function useSelectCellFactory<OBJECT, ATTRIBUTE extends DomainObject>(
  dataHook: () => ListSource<ATTRIBUTE>,
  DisabledComponent?: ComponentType<{
    value: string | null;
    disabled?: boolean;
  }>
) {
  return useMemo(
    () =>
      function SelectCell({ value }: TableFieldCellProps<OBJECT>) {
        const source = dataHook();

        return (
          <Select
            disabled={true}
            source={source}
            value={value}
            onChange={noop}
            valueIsId={true}
            DisabledComponent={DisabledComponent}
          />
        );
      },
    [dataHook, DisabledComponent]
  );
}
