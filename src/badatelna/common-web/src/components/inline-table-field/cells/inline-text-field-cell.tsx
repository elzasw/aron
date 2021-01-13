import React, { useMemo } from 'react';
import { TableFieldCellProps } from 'components/table-field/table-field-types';
import { FormTextField } from 'composite/form/fields/form-text-field';

export function useInlineTextFieldFactory<OBJECT>({
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
      function InlineTextFieldCell({
        index,
        column,
      }: TableFieldCellProps<OBJECT>) {
        return (
          <FormTextField
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
