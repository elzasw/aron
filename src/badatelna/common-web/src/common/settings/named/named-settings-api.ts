import { abortableFetch } from 'utils/abortable-fetch';
import { NamedSettings } from './named-settings-types';

/**
 * Load all named settings for specified tag.
 *
 * @param url Url of service
 */
export function fetchSettingsByTag(url: string, tag: string) {
  return abortableFetch(`${url}/by-tag/${tag}`, {
    method: 'GET',
  });
}

/**
 * Saves named settings.
 *
 * @param url Url of service
 */
export function createSettings(url: string, settings: NamedSettings) {
  return abortableFetch(url, {
    method: 'POST',
    headers: new Headers({
      'Content-Type': 'application/json',
    }),
    body: JSON.stringify(settings),
  });
}

/**
 * Clear named settings.
 *
 * @param meUrl Url of service
 */
export function deleteSettings(url: string, id: string) {
  return abortableFetch(`${url}/${id}`, {
    method: 'DELETE',
  });
}
