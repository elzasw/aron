import {
  TableFieldColumn,
  TableFieldProps,
  TableFieldFormFieldsProps,
} from 'components/table-field/table-field-types';
import { ComponentType } from 'react';

export interface NestedTableFieldProps<
  NESTED_PATH extends string,
  NESTED_OBJECT,
  OBJECT extends { [key in NESTED_PATH]: NESTED_OBJECT }
> extends TableFieldProps<OBJECT> {
  nestedPath: NESTED_PATH;
  nestedColumns: TableFieldColumn<NESTED_OBJECT>[];
  NestedFormFieldsComponent?: ComponentType<
    TableFieldFormFieldsProps<NESTED_OBJECT>
  >;
}
