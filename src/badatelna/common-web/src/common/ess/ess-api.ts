import { useStaticListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'common/common-types';

export function useComponentTypes() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'MAIN', name: 'Hlavní' },
    { id: 'DELIVERY_NOTE', name: 'Doručenka' },
  ]);
}

export function useDispatchMethods() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'EMAIL', name: 'Email' },
    { id: 'DATABOX', name: 'Datová schránka' },
    { id: 'POSTAL', name: 'Poštovní adresa' },
  ]);
}

export function useDispatchStates() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'CREATED', name: 'Vytvořeno' },
    { id: 'SENT', name: 'Odesláno' },
    { id: 'DELIVERED', name: 'Doručeno' },
  ]);
}
