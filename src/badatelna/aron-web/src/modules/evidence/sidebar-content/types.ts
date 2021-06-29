import { FilterChangeCallBack } from '../types';
import { FacetType, ModulePath } from '../../../enums';
import {
  ApiFilterOperation,
  Filter,
  Relationship,
  ApuPartItemType,
  FilterConfig,
  Facet,
  Option,
} from '../../../types';

interface GenericFilterProps {
  source: string;
  value: any;
  label?: string;
  tooltip?: string;
  description?: string;
  inDialog?: boolean;
  onChange: FilterChangeCallBack;
  operation?: ApiFilterOperation;
  apiFilters: Filter[];
}

export interface RangeFilterProps extends GenericFilterProps {
  type: FacetType;
  interval: [number, number];
}

export interface InputFilterProps extends GenericFilterProps {
  value: string;
}

export interface AutocompleteFilterProps extends GenericFilterProps {
  value: Option[];
  filters?: Filter[];
  multiple?: boolean;
  apuPartItemTypes: ApuPartItemType[];
}

export interface SelectionFilterProps extends GenericFilterProps {
  type: FacetType;
  options: SelectionFilterOption[];
  displayedItems?: number;
  maxDisplayedItems?: number;
  value: string[];
  filters: Filter[];
}

export interface SelectionFilterOption {
  label: string;
  value: any;
  tooltip?: string;
}

export interface RelationshipFilterProps extends GenericFilterProps {
  value: Relationship[];
  apuPartItemTypes: ApuPartItemType[];
  group?: string;
}

export interface RelationshipFilterCreatorProps {
  onChange: (newRelationships: Relationship[]) => void;
  apuPartItemTypes: ApuPartItemType[];
  apiFilters: Filter[];
  group?: string;
}

export interface FilterDialogProps {
  filters: FilterConfig[];
  onClose: () => void;
  apuPartItemTypes: ApuPartItemType[];
  facets: Facet[];
  path: ModulePath;
}
