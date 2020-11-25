import { DomainObject } from 'common/common-types';
import { Select } from 'components/select/select';
import { SelectProps } from 'components/select/select-types';
import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { formFieldFactory } from './form-field';

export const FormSelect = formFieldFactory(Select) as <
  OPTION extends DomainObject
>(
  props: FormFieldProps<SelectProps<OPTION>>
) => JSX.Element;
