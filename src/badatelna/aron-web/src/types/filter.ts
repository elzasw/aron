import { ApuType, FilterType } from '../enums';

export interface FilterOption {
  value: string;
  label: string;
}

export interface FilterConfig {
  type?: FilterType;
  label?: string;
  field: string;
  value: string[];
  options?: any[];
}

export interface FavouriteQuery {
  icon: any;
  label: string;
  query?: string;
  type?: ApuType;
  filters?: FilterConfig[];
}

interface FacetInterval {
  from: string;
  fromText: string;
  to: string;
  toText: string;
}

export enum FacetType {
  FULLTEXT = 'FULLTEXT',
  ENUM = 'ENUM',
  MULTI_REF = 'MULTI_REF',
  MULTI_REF_EXT = 'MULTI_REF_EXT',
  UNITDATE = 'UNITDATE',
}

export interface Facet {
  display: 'ALWAYS' | 'DETAIL';
  facets: Facet[];
  intervals: FacetInterval[];
  maxItems: number;
  source: string;
  type: FacetType;
  when: { apuType: ApuType };
}

export interface AggregationItem {
  key: string;
  value: string;
  [label: string]: string; // label property in form `${key}~LABEL`
}
export interface AggregationItems {
  [name: string]: AggregationItem[];
}
export interface Relationship {
  field: string;
  value: string;
}
