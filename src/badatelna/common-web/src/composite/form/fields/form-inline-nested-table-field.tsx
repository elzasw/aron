import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { formFieldFactory } from './form-field';
import { NestedInlineTableField } from 'components/nested-inline-table-field/nested-inline-table-field';
import { NestedInlineTableFieldProps } from 'components/nested-inline-table-field/nested-inline-table-field-types';

export const FormNestedInlineTableField = formFieldFactory(
  NestedInlineTableField
) as <
  NESTED_PATH extends string,
  NESTED_OBJECT,
  OBJECT extends { [key in NESTED_PATH]: NESTED_OBJECT }
>(
  props: FormFieldProps<
    NestedInlineTableFieldProps<NESTED_PATH, NESTED_OBJECT, OBJECT>
  >
) => JSX.Element;
