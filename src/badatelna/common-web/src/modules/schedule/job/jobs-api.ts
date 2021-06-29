import { abortableFetch } from 'utils/abortable-fetch';

export function startCall(url: string, id: string) {
  return abortableFetch(`${url}/${id}/start`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'POST',
  });
}

export function cancelCall(url: string, id: string) {
  return abortableFetch(`${url}/${id}/cancel`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'POST',
  });
}
