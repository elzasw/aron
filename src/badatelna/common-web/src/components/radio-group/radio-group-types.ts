import { DomainObject, ListSource } from 'common/common-types';

export interface RadioGroupProps<OPTION extends DomainObject> {
  disabled?: boolean;
  value?: string | OPTION | null;
  onChange: (value: string | OPTION | null) => void;
  source: ListSource<OPTION>;
  valueIsId?: boolean;
  idMapper?: (option: OPTION) => string;
  labelMapper?: (option: OPTION) => string;
  form?: string;
}
