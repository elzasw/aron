import React, { useMemo } from 'react';
import { DomainObject, ListSource } from 'common/common-types';
import { TableFieldCellProps } from 'components/table-field/table-field-types';
import { FormSelect } from 'composite/form/fields/form-select';

export function useInlineSelectCellFactory<
  OBJECT,
  ATTRIBUTE extends DomainObject
>({
  dataHook,
  collectionDatakey,
  isSubkey = true,
  multiple,
  valueIsId,
  disabled,
}: {
  dataHook: () => ListSource<ATTRIBUTE>;
  collectionDatakey: string;
  isSubkey?: boolean;
  multiple?: boolean;
  valueIsId?: boolean;
  disabled?: ((index: number) => boolean) | boolean;
}) {
  return useMemo(
    () =>
      function InlineSelectCell({
        index,
        column,
      }: TableFieldCellProps<OBJECT>) {
        const source = dataHook();

        return (
          <FormSelect
            name={
              isSubkey
                ? `${collectionDatakey}[${index}].${column.datakey}`
                : `${collectionDatakey}[${index}]`
            }
            source={source}
            multiple={multiple}
            labelOptions={{ hide: true }}
            layoutOptions={{ noUnderline: true }}
            valueIsId={valueIsId}
            disabled={
              typeof disabled === 'function' ? disabled(index) : disabled
            }
          />
        );
      },
    [collectionDatakey, dataHook, isSubkey, multiple, valueIsId, disabled]
  );
}
