import { AbortableFetch } from 'utils/abortable-fetch';

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
  'FTXF' = 'FTXF',
  'AND' = 'AND',
  'OR' = 'OR',
  'NOT' = 'NOT',
  'CUSTOM' = 'CUSTOM',
  'IDS' = 'IDS',
  'AKF' = 'AKF',
  'RANGE' = 'RANGE',
}

export interface Filter {
  field?: string;
  value?: any;
  values?: any[];
  operation: ApiFilterOperation;
  ids?: string[];
  filters?: Filter[];
}

export interface Sort {
  field: string;
  type: 'FIELD' | 'GEO_DISTANCE' | 'SCRIPT' | 'SCORE';
  order: 'ASC' | 'DESC';

  /**
   * Points, that will be used with GEO_DISTANCE filter
   */
  points?: { lat?: number; lon?: number }[];

  /**
   * Sorting mode for GEO_DISTANCE filter
   */
  sortMode?: 'MIN' | 'MAX' | 'SUM' | 'AVG' | 'MEDIAN';
}

export interface Params {
  size?: number;
  sort?: Sort[];
  flipDirection?: boolean;
  filters?: Filter[];
  include?: string[];
  searchAfter?: string;
}

export interface ListSource<TYPE> {
  items: TYPE[];
  loading: boolean;
  reset: () => void;
  loadDetail: (item: TYPE) => Promise<TYPE>;
}

export interface ResultDto<TYPE> {
  items: TYPE[];
  count: number;
  searchAfter?: string;
}

export interface Source<TYPE> extends ResultDto<TYPE> {
  loading: boolean;
  setLoading: (loading: boolean) => void;
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
  getParams: () => Record<string, any>;
  loadMore: () => Promise<void>;
}

export interface CrudSourceProps<TYPE extends DomainObject> {
  url: string;
  getItem?: (api: string, itemId: string) => AbortableFetch;
  createItem?: (api: string, item: TYPE) => AbortableFetch;
  updateItem?: (api: string, item: TYPE, _initialItem: TYPE) => AbortableFetch;
  deleteItem?: (api: string, itemId: string) => AbortableFetch;

  handleGetError?: (err: Error) => void;
  handleCreateError?: (err: Error) => void;
  handleUpdateError?: (err: Error) => void;
  handleDeleteError?: (err: Error) => void;

  getMessages?: {
    errorMessage?: string;
  };

  createMessages?: {
    successMessage?: string;
    errorMessage?: string;
  };

  updateMessages?: {
    successMessage?: string;
    errorMessage?: string;
  };

  delMessages?: {
    successMessage?: string;
    errorMessage?: string;
  };
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
  reset: (data?: TYPE) => void;
  setLoading: (loading: boolean) => void;
}

export interface FileRef extends AuthoredObject {
  name: string;
  contentType: string;
  size: number;
}

export enum ScriptType {
  GROOVY = 'GROOVY',
  JAVASCRIPT = 'JAVASCRIPT',
}
