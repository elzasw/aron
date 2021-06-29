import { abortableFetch } from 'utils/abortable-fetch';

export function startCall(api: string, id: string) {
  return abortableFetch(`${api}/${id}/start`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'POST',
  });
}

export function cancelCall(api: string, id: string) {
  return abortableFetch(`${api}/${id}/cancel`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'POST',
  });
}
