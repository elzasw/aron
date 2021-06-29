import React, { useMemo } from 'react';
import { TableFieldCellProps } from 'components/table-field/table-field-types';
import { FormCheckbox } from 'composite/form/fields/form-checkbox';

export function useInlineCheckboxFactory<OBJECT>({
  collectionDatakey,
  isSubkey = true,
  disabled,
}: {
  collectionDatakey: string;
  isSubkey?: boolean;
  disabled?: ((index: number) => boolean) | boolean;
}) {
  return useMemo(
    () =>
      function InlineCheckboxCell({
        index,
        column,
      }: TableFieldCellProps<OBJECT>) {
        return (
          <FormCheckbox
            name={
              isSubkey
                ? `${collectionDatakey}[${index}].${column.datakey}`
                : `${collectionDatakey}[${index}]`
            }
            labelOptions={{ hide: true }}
            layoutOptions={{ noUnderline: true }}
            disabled={
              typeof disabled === 'function' ? disabled(index) : disabled
            }
          />
        );
      },
    [collectionDatakey, isSubkey, disabled]
  );
}
