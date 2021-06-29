import { DictionaryAutocomplete } from 'common/common-types';

export function dictionaryColumnMapper({
  value,
}: {
  value: DictionaryAutocomplete | undefined;
}) {
  return value?.name;
}

dictionaryColumnMapper.displayName = 'dictionaryColumnMapper';

export function dictionaryArrayColumnMapper({
  value,
}: {
  value: DictionaryAutocomplete[] | undefined;
}) {
  return (value || []).map((v) => v.name).join(', ');
}

dictionaryArrayColumnMapper.displayName = 'dictionaryArrayColumnMapper';
