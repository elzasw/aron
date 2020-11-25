import { abortableFetch } from 'utils/abortable-fetch';

export function callLoadTranslations(translationsUrl: string, lang: string) {
  return abortableFetch(`${translationsUrl}/load/${lang}`, {
    method: 'GET',
  });
}
