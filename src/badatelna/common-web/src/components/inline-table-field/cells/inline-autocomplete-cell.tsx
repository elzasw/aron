import React, { useMemo } from 'react';
import { DomainObject } from 'common/common-types';
import { TableFieldCellProps } from 'components/table-field/table-field-types';
import { FormAutocomplete } from 'composite/form/fields/form-autocomplete';
import { AutocompleteSource } from 'components/autocomplete/autocomplete-types';

export function useInlineAutocompleteCellFactory<
  OBJECT,
  ATTRIBUTE extends DomainObject
>({
  dataHook,
  collectionDatakey,
  isSubkey = true,
  multiple,
  disabled,
}: {
  dataHook: () => AutocompleteSource<ATTRIBUTE>;
  collectionDatakey: string;
  isSubkey?: boolean;
  multiple?: boolean;
  disabled?: ((index: number) => boolean) | boolean;
}) {
  return useMemo(
    () =>
      function InlineAutocompleteCell({
        index,
        column,
      }: TableFieldCellProps<OBJECT>) {
        const source = dataHook();

        return (
          <FormAutocomplete
            name={
              isSubkey
                ? `${collectionDatakey}[${index}].${column.datakey}`
                : `${collectionDatakey}[${index}]`
            }
            source={source}
            multiple={multiple}
            labelOptions={{ hide: true }}
            layoutOptions={{ noUnderline: true }}
            disabled={
              typeof disabled === 'function' ? disabled(index) : disabled
            }
          />
        );
      },
    [collectionDatakey, dataHook, isSubkey, multiple, disabled]
  );
}
