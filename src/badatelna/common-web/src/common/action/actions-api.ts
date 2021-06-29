import { useStaticListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'common/common-types';

export function useScriptTypes() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'GROOVY', name: 'Groovy' },
    { id: 'JAVASCRIPT', name: 'Javascript' },
  ]);
}
