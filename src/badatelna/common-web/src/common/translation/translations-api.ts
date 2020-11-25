import { useStaticListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'index';

export function useLanguages() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'CZECH', name: 'Česky' },
    { id: 'ENGLISH', name: 'English' },
    { id: 'GERMAN', name: 'Deutch' },
    { id: 'SLOVAK', name: 'Slovensky' },
  ]);
}
