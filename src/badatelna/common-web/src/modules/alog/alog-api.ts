import { useStaticListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'common/common-types';

export function useEventSourceTypes() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'USER', name: 'Uživatel' },
    { id: 'SYSTEM', name: 'Systém' },
    { id: 'EXTERNAL', name: 'Externí systém' },
  ]);
}
