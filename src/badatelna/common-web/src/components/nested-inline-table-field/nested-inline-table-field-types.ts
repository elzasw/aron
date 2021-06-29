import { TableFieldColumn } from 'components/table-field/table-field-types';
import { InlineTableFieldProps } from 'components/inline-table-field/inline-table-field-types';

export interface NestedInlineTableFieldProps<
  NESTED_PATH extends string,
  NESTED_OBJECT,
  OBJECT extends { [key in NESTED_PATH]: NESTED_OBJECT }
> extends InlineTableFieldProps<OBJECT> {
  nestedPath: NESTED_PATH;
  nestedColumns: TableFieldColumn<NESTED_OBJECT>[];
}
