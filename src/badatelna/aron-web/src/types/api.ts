// TODO: later: import from common-web

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
  'AKF' = 'AKF',
}

export interface Filter {
  field?: string;
  value?: any;
  operation: ApiFilterOperation;
  ids?: string[];
  filters?: Filter[];
}
