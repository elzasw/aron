import React, { useMemo } from 'react';
import { compact } from 'lodash';
import { TableFieldColumn } from 'components/table-field/table-field-types';
import { NestedInlineTableFieldProps } from './nested-inline-table-field-types';
import { InlineTableField } from 'components/inline-table-field/inline-table-field';

export function NestedInlineTableField<
  NESTED_PATH extends string,
  NESTED_OBJECT,
  OBJECT extends { [key in NESTED_PATH]: NESTED_OBJECT }
>({
  nestedPath,
  nestedColumns,
  columns: directColumns,
  ...props
}: NestedInlineTableFieldProps<NESTED_PATH, NESTED_OBJECT, OBJECT>) {
  const columns = useMemo(
    () =>
      compact([
        ...directColumns,
        ...nestedColumns.map((column) => ({
          ...column,
          datakey: `${nestedPath}.${column.datakey}`,
        })),
      ]) as TableFieldColumn<OBJECT>[],
    [directColumns, nestedColumns, nestedPath]
  );

  return <InlineTableField {...props} columns={columns} />;
}
