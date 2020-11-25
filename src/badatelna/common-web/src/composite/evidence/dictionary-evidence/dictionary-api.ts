import { abortableFetch } from 'utils/abortable-fetch';

/**
 * Calls activate API method.
 *
 *
 * @param api API endpoint
 * @param id Id of the item
 */
export function activateItem(api: string, id: string) {
  return abortableFetch(`${api}/${id}/active`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'PUT',
  });
}

/**
 * Calls deactivate API method.
 *
 *
 * @param api API endpoint
 * @param id Id of the item
 */
export function deactivateItem(api: string, id: string) {
  return abortableFetch(`${api}/${id}/active`, {
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    method: 'DELETE',
  });
}
