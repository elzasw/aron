import { useStaticListSource } from 'utils/list-source-hook';
import { DictionaryAutocomplete } from 'common/common-types';
import { abortableFetch } from 'utils/abortable-fetch';
import { UploadSignedContentDto } from './signing-types';

export function useRequestStates() {
  return useStaticListSource<DictionaryAutocomplete>([
    { id: 'NEW', name: 'Nový' },
    { id: 'SIGNED', name: 'Podepsaný' },
    { id: 'CANCELED', name: 'Zrušený' },
    { id: 'ERROR', name: 'Chyba' },
  ]);
}

export function uploadSignedContentFactory(url: string) {
  return function uploadSignedContent(id: string, dto: UploadSignedContentDto) {
    return abortableFetch(`${url}/${id}/upload-signed-content`, {
      headers: new Headers({
        'Content-Type': 'application/json',
      }),
      method: 'POST',
      body: JSON.stringify(dto),
    });
  };
}

export function signFactory(url: string) {
  return function sign(id: string) {
    return abortableFetch(`${url}/${id}/sign`, {
      headers: new Headers({
        'Content-Type': 'application/json',
      }),
      method: 'POST',
    });
  };
}
