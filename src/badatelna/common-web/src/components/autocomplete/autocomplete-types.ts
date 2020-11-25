import { DomainObject, ScrollableSource } from 'common/common-types';
import { ReactNode } from 'react';

export interface AutocompleteSource<OPTION> extends ScrollableSource<OPTION> {
  setSearchQuery: (q: string) => void;
}

export interface AutocompleteProps<OPTION extends DomainObject> {
  disabled?: boolean;
  value: OPTION | OPTION[] | null | undefined;
  onChange: (value: OPTION | OPTION[] | null) => void;

  source: AutocompleteSource<OPTION>;
  clearable?: boolean;

  multiple?: boolean;
  showTooltip?: boolean;

  labelMapper?: (option: OPTION) => string;
  tooltipMapper?: (option: OPTION) => ReactNode;
}
