import { DictionaryObject, ScriptType } from 'common/common-types';

export interface Action extends DictionaryObject {
  scriptType: ScriptType;
  script: string;
}
