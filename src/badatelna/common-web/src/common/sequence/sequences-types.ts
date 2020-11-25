import { DictionaryObject } from 'common/common-types';

export interface Sequence extends DictionaryObject {
  description: string;
  format: string;
  counter: number;
  local: boolean;
}
