import React, { useMemo } from 'react';
import { TableFieldCellProps } from 'components/table-field/table-field-types';
import { FormDateField } from 'composite/form/fields/form-date-field';

export function useInlineDateFieldFactory<OBJECT>({
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
      function InlineDateFieldCell({
        index,
        column,
      }: TableFieldCellProps<OBJECT>) {
        return (
          <FormDateField
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
