import React, { useMemo } from 'react';
import { compact } from 'lodash';
import { TableField } from 'components/table-field/table-field';
import {
  TableFieldColumn,
  TableFieldFormFieldsProps,
  TableFieldCellProps,
} from 'components/table-field/table-field-types';
import { FormFieldContext } from 'composite/form/fields/form-field-context';
import { TableFieldCells } from 'components/table-field/table-field-cells';
import { NestedTableFieldProps } from './nested-table-field-types';

export function OrderColumnCell(props: TableFieldCellProps<any>) {
  return <TableFieldCells.TextCell {...props} value={`${props.index}`} />;
}

export function NestedTableField<
  NESTED_PATH extends string,
  NESTED_OBJECT,
  OBJECT extends { [key in NESTED_PATH]: NESTED_OBJECT }
>({
  nestedPath,
  nestedColumns,
  columns: directColumns,
  NestedFormFieldsComponent,
  FormFieldsComponent: DirectFormFieldsComponent,
  ...props
}: NestedTableFieldProps<NESTED_PATH, NESTED_OBJECT, OBJECT>) {
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

  const FormFieldsComponent = useMemo(
    () =>
      function FormFieldsComponent(props: TableFieldFormFieldsProps<OBJECT>) {
        return (
          <>
            {DirectFormFieldsComponent && (
              <DirectFormFieldsComponent {...props} />
            )}
            <FormFieldContext.Provider value={{ prefix: nestedPath }}>
              {NestedFormFieldsComponent && (
                <NestedFormFieldsComponent
                  initialValue={props.initialValue?.[nestedPath] ?? null}
                />
              )}
            </FormFieldContext.Provider>
          </>
        );
      },
    [DirectFormFieldsComponent, NestedFormFieldsComponent, nestedPath]
  );

  return (
    <TableField
      {...props}
      columns={columns}
      FormFieldsComponent={FormFieldsComponent}
    />
  );
}
