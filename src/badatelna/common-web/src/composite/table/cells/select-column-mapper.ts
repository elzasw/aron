import { useMemo } from 'react';
import { ListSource, DictionaryAutocomplete } from 'common/common-types';

export function useSelectCellFactory(
  dataHook: () => ListSource<DictionaryAutocomplete>
) {
  const source = dataHook();

  return useMemo(
    () =>
      function selectColumnMapper({ value }: { value: string | undefined }) {
        const item = source.items.find((v) => v.id === value);
        return item?.name;
      },
    [source.items]
  );
}
