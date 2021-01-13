import { abortableFetch } from 'utils/abortable-fetch';
import { UserSettings } from './user-settings-types';

/**
 * Load user settings.
 *
 * @param meUrl Url of service
 */
export function fetchSettings(url: string) {
  return abortableFetch(url, {
    method: 'GET',
  });
}

/**
 * Saves user settings.
 *
 * @param meUrl Url of service
 */
export function updateSettings(url: string, settings: UserSettings) {
  return abortableFetch(url, {
    method: 'PUT',
    body: JSON.stringify(settings),
  });
}
