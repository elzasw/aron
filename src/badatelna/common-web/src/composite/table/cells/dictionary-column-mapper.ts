import { DictionaryAutocomplete } from 'common/common-types';

export function dictionaryColumnMapper({
  value,
}: {
  value: DictionaryAutocomplete | undefined;
}) {
  return value?.name;
}

export function dictionaryArrayColumnMapper({
  value,
}: {
  value: DictionaryAutocomplete[] | undefined;
}) {
  return (value || []).map((v) => v.name).join(', ');
}
