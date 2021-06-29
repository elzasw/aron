import { abortableFetch } from 'utils/abortable-fetch';

export function sendFactory(url: string) {
  return function send(id: string) {
    return abortableFetch(`${url}/${id}/send`, {
      headers: new Headers({
        'Content-Type': 'application/json',
      }),
      method: 'POST',
    });
  };
}

export function deliverFactory(url: string) {
  return function deliver(id: string) {
    return abortableFetch(`${url}/${id}/deliver`, {
      headers: new Headers({
        'Content-Type': 'application/json',
      }),
      method: 'POST',
    });
  };
}
