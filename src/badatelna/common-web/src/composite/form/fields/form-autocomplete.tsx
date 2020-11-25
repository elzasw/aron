import { DomainObject } from 'common/common-types';
import { Autocomplete } from 'components/autocomplete/autocomplete';
import { AutocompleteProps } from 'components/autocomplete/autocomplete-types';
import { FormFieldProps } from './wrapper/form-field-wrapper-types';
import { formFieldFactory } from './form-field';

export const FormAutocomplete = formFieldFactory(Autocomplete) as <
  OPTION extends DomainObject
>(
  props: FormFieldProps<AutocompleteProps<OPTION>>
) => JSX.Element;
