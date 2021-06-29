import { useMemo } from 'react';
import { ListSource, DictionaryAutocomplete } from 'common/common-types';

export function useSelectCellFactory(
  dataHook: () => ListSource<DictionaryAutocomplete>
) {
  const source = dataHook();

  return useMemo(() => {
    const selectColumnMapper = function selectColumnMapper({
      value,
    }: {
      value: string | undefined;
    }) {
      const item = source.items.find((v) => v.id === value);
      return item?.name;
    };

    // store current items into the function object for easy retrieval during export generation
    selectColumnMapper.displayName = 'selectColumnMapper';
    selectColumnMapper.data = source.items;

    return selectColumnMapper;
  }, [source.items]);
}
