import { ApiFilterOperation } from '@eas/common-web';

export interface Filter {
  field?: string;
  value?: any;
  operation: ApiFilterOperation;
  ids?: string[];
  filters?: Filter[];
}
