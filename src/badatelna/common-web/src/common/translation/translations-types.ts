import { DictionaryObject } from 'common/common-types';

export enum Langugage {
  CZECH = 'CZECH',
  ENGLISH = 'ENGLISH',
  GERMAN = 'GERMAN',
  SLOVAK = 'SLOVAK',
}

export interface Translation extends DictionaryObject {
  language: Langugage;
}
