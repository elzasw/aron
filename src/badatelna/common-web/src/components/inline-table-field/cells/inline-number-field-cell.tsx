import React, { useMemo } from 'react';
import { TableFieldCellProps } from 'components/table-field/table-field-types';
import { FormNumberField } from 'composite/form/fields/form-number-field';

export function useInlineNumberFieldFactory<OBJECT>({
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
      function InlineNumberFieldCell({
        index,
        column,
      }: TableFieldCellProps<OBJECT>) {
        return (
          <FormNumberField
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
