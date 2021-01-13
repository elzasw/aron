import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { formFieldFactory } from './form-field';
import { InlineTableField } from 'components/inline-table-field/inline-table-field';
import { InlineTableFieldProps } from 'components/inline-table-field/inline-table-field-types';

export const FormInlineTableField = formFieldFactory(InlineTableField) as <
  OBJECT
>(
  props: FormFieldProps<InlineTableFieldProps<OBJECT>>
) => JSX.Element;
