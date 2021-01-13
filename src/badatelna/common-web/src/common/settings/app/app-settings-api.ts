import { abortableFetch } from 'utils/abortable-fetch';
import { AppSettings } from './app-settings-types';

/**
 * Load user settings.
 *
 * @param url Url of service
 */
export function fetchSettings(url: string) {
  return abortableFetch(url, {
    method: 'GET',
  });
}

/**
 * Saves user settings.
 *
 * @param url Url of service
 */
export function updateSettings(url: string, settings: AppSettings) {
  return abortableFetch(url, {
    method: 'PUT',
    body: JSON.stringify(settings),
  });
}
