export interface DomainObject {
  id: string;
}

export interface DatedObject extends DomainObject {
  created?: string;
  updated?: string;
  deleted?: string;
}

export interface UserReference {
  id: string;
  name: string;
}

export interface TenantReference {
  id: string;
  name: string;
}

export interface AuthoredObject extends DatedObject {
  createdBy?: UserReference;
  createdByTenant?: TenantReference;

  updatedBy?: UserReference;
  updatedByTenant?: TenantReference;

  deletedBy?: UserReference;
  deletedByTenant?: TenantReference;
}

export interface DictionaryObject extends AuthoredObject {
  name: string;
  active?: boolean;
  validFrom?: string;
  validTo?: string;
  order?: number;
  code?: string;
}

export interface DictionaryAutocomplete {
  id: string;
  name: string;
  code?: string;
}

export enum ApiFilterOperation {
  'EQ' = 'EQ',
  'START_WITH' = 'START_WITH',
  'END_WITH' = 'END_WITH',
  'CONTAINS' = 'CONTAINS',
  'NOT_NULL' = 'NOT_NULL',
  'IS_NULL' = 'IS_NULL',
  'GT' = 'GT',
  'LT' = 'LT',
  'GTE' = 'GTE',
  'LTE' = 'LTE',
  'FTX' = 'FTX',
  'AND' = 'AND',
  'OR' = 'OR',
  'NOT' = 'NOT',
  'CUSTOM' = 'CUSTOM',
  'IDS' = 'IDS',
}

export interface Filter {
  field?: string;
  value?: any;
  operation: ApiFilterOperation;
  ids?: string[];
  filters?: Filter[];
}

export interface Sort {
  field: string;
  type: 'FIELD' | 'GEO_DISTANCE' | 'SCRIPT' | 'SCORE';
  order: 'ASC' | 'DESC';
}

export interface Params {
  size?: number;
  sort?: Sort[];
  flipDirection?: boolean;
  filters?: Filter[];
  searchAfter?: string;
}

export interface ListSource<TYPE> {
  items: TYPE[];
  loading: boolean;
  reset: () => void;
}

export interface ResultDto<TYPE> {
  items: TYPE[];
  count: number;
  searchAfter?: string;
}

export interface Source<TYPE> extends ResultDto<TYPE> {
  loading: boolean;
  reset: () => void;
}

export interface BodySource<TYPE, BODY> extends Source<TYPE> {
  hasNextPage: () => boolean;
  isDataValid: () => boolean;
  setBody: (body: BODY) => void;
  loadMore: () => Promise<void>;
}

export interface ScrollableSource<TYPE> extends Source<TYPE> {
  hasNextPage: () => boolean;
  isDataValid: () => boolean;
  setParams: (params: Params) => void;
  loadMore: () => Promise<void>;
}

export interface CrudSource<TYPE extends DomainObject> {
  url: string;
  data: TYPE | null;
  loading: boolean;
  get: (id: string) => Promise<TYPE | undefined>;
  create: (obj: TYPE) => Promise<TYPE | undefined>;
  update: (obj: TYPE, prevObj: TYPE) => Promise<TYPE | undefined>;
  del: (id: string) => Promise<void>;
  refresh: () => Promise<TYPE | undefined>;
  reset: () => void;
  setLoading: (loading: boolean) => void;
}

export interface FileRef extends AuthoredObject {
  name: string;
  contentType: string;
  size: number;
}