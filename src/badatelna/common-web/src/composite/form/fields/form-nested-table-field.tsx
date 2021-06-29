import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { formFieldFactory } from './form-field';
import { NestedTableField } from 'components/nested-table-field/nested-table-field';
import { NestedTableFieldProps } from 'components/nested-table-field/nested-table-field-types';

export const FormNestedTableField = formFieldFactory(NestedTableField) as <
  NESTED_PATH extends string,
  NESTED_OBJECT,
  OBJECT extends { [key in NESTED_PATH]: NESTED_OBJECT }
>(
  props: FormFieldProps<
    NestedTableFieldProps<NESTED_PATH, NESTED_OBJECT, OBJECT>
  >
) => JSX.Element;
