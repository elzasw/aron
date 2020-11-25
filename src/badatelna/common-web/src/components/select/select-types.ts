import { ReactNode } from 'react';
import { DomainObject, ListSource } from 'common/common-types';

export interface SelectProps<OPTION extends DomainObject> {
  disabled?: boolean;
  value: string | string[] | OPTION | OPTION[] | null | undefined;
  onChange: (value: string | string[] | OPTION | OPTION[] | null) => void;

  source: ListSource<OPTION>;

  valueIsId?: boolean;
  multiple?: boolean;
  showTooltip?: boolean;

  /**
   * Does this control have clear button?
   *
   * Default true
   */
  clearable?: boolean;

  /**
   * Does this control have select all button if it is multiple?
   *
   * Default true
   */
  selectableAll?: boolean;

  idMapper?: (option: OPTION) => string;
  labelMapper?: (option: OPTION) => ReactNode;
  tooltipMapper?: (option: OPTION) => ReactNode;
}
